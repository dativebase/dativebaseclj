(ns dvb.server.http.client-e2e-test
  (:require [clojure.pprint :as pp]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [com.stuartsierra.component.repl :as component.repl]
            [dvb.common.openapi.spec :as spec]
            [dvb.common.specs.users :as users-specs]
            [dvb.common.specs.olds :as olds-specs]
            [dvb.client.core :as client]
            [dvb.server.core :as core]
            [dvb.server.db.events :as db.events]
            [dvb.server.db.forms :as db.forms]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.test-queries :as test-queries]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.openapi.serialize :as openapi.serialize]
            [dvb.server.init :as init]
            [dvb.server.sh :as sh]
            [dvb.server.system.config :as config]
            [java-time.api :as jt]))

(defn new-system []
  (core/make-main-system (assoc (config/init config/dev-config-path)
                                :server-port 8087)))

(defn setup [database]
  (let [user-defaults {:is-superuser? false
                       :created-by nil
                       :updated-by nil}
        {su-pwd :password :as superuser*}
        (merge (gen/generate (s/gen ::users-specs/user))
               user-defaults {:is-superuser? true})
        superuser (db.users/activate-user
                   database
                   (db.users/create-user database superuser*))
        {u-pwd :password :as user*}
        (merge (gen/generate (s/gen ::users-specs/user)) user-defaults)
        user (db.users/activate-user
              database
              (db.users/create-user database user*))
        {old-slug :slug :as old*}
        (merge (gen/generate (s/gen ::olds-specs/old))
               {:created-by nil :updated-by nil})
        old (db.olds/create-old database old*)
        user-old (db.users/create-user-old
                  database {:user-id (:id user)
                            :old-slug old-slug
                            :role "contributor"
                            :created-by nil
                            :updated-by nil})
        superuser-old (db.users/create-user-old
                       database {:user-id (:id superuser)
                                 :old-slug old-slug
                                 :role "administrator"
                                 :created-by nil
                                 :updated-by nil})]
    {:user user
     :user-password u-pwd
     :superuser superuser
     :superuser-password su-pwd
     :old old}))

;; Long-running end-to-end test that verifies:
;; - POST users (C)
;; - GET users (R)
;; - GET users/:id (R)
;; - PUT users/:id (U)
;; - DELETE users/:id (D)
(deftest users-endpoint-works-end-to-end
  (let [{:keys [database] :as system} (component/start (new-system))]
    (try
      (let [{:keys [user user-password superuser superuser-password old]
             :as data} (setup database)
            superuser-email (:email superuser)
            user-email (:email user)
            old-slug (:slug old)
            superuser-client (client/authenticate-client
                              (client/make-client :local-test)
                              superuser-email superuser-password)
            user-client (client/authenticate-client
                         (client/make-client :local-test)
                         user-email user-password)]
        (testing "Directly-constructed users have no creators or updaters"
          (is (nil? (:created-by superuser :empty)))
          (is (nil? (:updated-by superuser :empty)))
          (is (nil? (:created-by user :empty)))
          (is (nil? (:updated-by user :empty))))
        (testing "We can authenticated with a superuser"
          (is (:authenticated? superuser-client)))
        (testing "We can authenticated with a user"
          (is (:authenticated? user-client)))
        (testing "We can index the users with the superuser-authenticated client"
          (let [{:as users-response :keys [status body]}
                (client/index-users superuser-client)]
            (is (= 200 status))
            (is (seq (:data body)))))
        ;; Seems undesired bad behaviour
        (testing "The user-authenticated client is not authorized to index the users"
          (let [{:as users-response :keys [status body]}
                (client/index-users user-client)]
            (is (= 403 status))
            (is (= "unauthorized" (-> body :errors first :error-code)))
            {:errors
             [{:message
               "The authenticated user is not authorized for the target operation.",
               :error-code "unauthorized"}]}))
        (testing "The superuser-authenticated client can create a new user"
          (let [new-user-password "1234"
                {:as new-user-response :keys [status] created-user :body}
                (client/create-user superuser-client
                                    {:password new-user-password})]
            (is (= 201 status))
            (is (uuid? (:id created-user)))
            (is (jt/instant? (:created-at created-user)))
            (is (jt/instant? (:updated-at created-user)))
            (is (nil? (:destroyed-at created-user)))
            (is (uuid? (:id created-user)))
            (testing "The superuser-authenticated client can fetch the newly created user"
              (let [{:as fetched-user-response :keys [status] fetched-user :body}
                    (client/show-user superuser-client (:id created-user))]
                (is (= created-user fetched-user))))
            (testing "The superuser-authenticated client can update the newly created user"
              (let [{:as updated-user-response :keys [status] updated-user :body}
                    (client/update-user
                     superuser-client
                     (:id created-user)
                     {:first-name "Timothy"})]
                (is (= (-> created-user
                           (assoc :first-name "Timothy")
                           (dissoc :updated-at))
                       (dissoc updated-user :updated-at)))
                (testing "The non-superuser-authenticated client cannot fetch the newly-created user"
                  (let [{:as fetched-user-response :keys [status] error :body}
                        (client/show-user user-client (:id created-user))]
                    (is (= 403 status))
                    (is (= "unauthorized" (-> error :errors first :error-code)))))
                (testing "The non-superuser-authenticated client cannot update the newly-created user"
                  (let [{:as updated-user-response :keys [status] error :body}
                        (client/update-user
                         user-client
                         (:id created-user)
                         {:first-name "Danuary"})]
                    (is (= 403 status))
                    (is (= "unauthorized" (-> error :errors first :error-code)))))
                (testing "It is not possible to authenticate with the newly-created user because it has not yet been activated"
                  (let [{:as failed-login-response :keys [status] error :body}
                        (client/login
                         (client/make-client :local-test)
                         (:email created-user)
                         new-user-password)]
                    (is (= 401 status))
                    (is (= "unregistered-user" (-> error :errors first :error-code)))))
                (testing "We can activate the user. Note: the registration key would be emailed to the user after initial signup, in the normal course of events."
                  (let [user-from-db (db.users/get-user database (:id created-user))
                        {:as activation-response :keys [status] activated-user :body}
                        (client/activate-user
                         (client/make-client :local-test)
                         (:id created-user)
                         (:registration-key user-from-db))]
                    (is (= 200 status))
                    (is (= (dissoc updated-user :updated-at)
                           (dissoc activated-user :updated-at)))))
                (testing "It is possible to authenticate with the newly-activated user"
                  (let [new-client (client/authenticate-client
                                    (client/make-client :local-test)
                                    (:email created-user)
                                    new-user-password)]
                    (is (:authenticated? superuser-client))))
                (testing "The superuser-authenticated client can delete the newly-created user"
                  (let [{:as deleted-user-response :keys [status] deleted-user :body}
                        (client/delete-user superuser-client (:id updated-user))
                        samer (fn [u] (dissoc u :updated-at :destroyed-at))]
                    (is (= (samer updated-user) (samer deleted-user)))
                    (is (some? (:destroyed-at deleted-user))))))))))
      (finally (component/stop system)))))
