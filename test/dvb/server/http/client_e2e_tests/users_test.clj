(ns dvb.server.http.client-e2e-tests.users-test
  (:require [bond.james :as bond]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [dvb.client.core :as client]
            [dvb.common.specs.users :as user-specs]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.client-e2e-tests.utils :as u]
            [dvb.server.http.operations.create-user :as create-user]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.system.clock :as clock]
            [java-time.api :as jt]))

(deftest users-endpoints-work-end-to-end
  (with-redefs [create-user/minimum-seconds-between-anonymous-user-creation 0]
    (let [{:keys [database] :as system} (component/start (u/new-system))]
      (try
        (let [{:keys [user user-password superuser superuser-password]}
              (u/setup database)
              {su-email :email su-id :id} superuser
              user-email (:email user)
              superuser-client (client/authenticate-client
                                (client/make-client :local-test)
                                su-email superuser-password)
              user-client (client/authenticate-client
                           (client/make-client :local-test)
                           user-email user-password)]
          (testing "Directly-constructed users have no creators or updaters."
            (is (nil? (:created-by superuser :empty)))
            (is (nil? (:updated-by superuser :empty))))
          (testing "User was created by superuser."
            (is (= su-id (:created-by user)))
            (is (= su-id (:updated-by user))))
          (testing "We can authenticated with a superuser."
            (is (:authenticated? superuser-client)))
          (testing "We can authenticated with a user."
            (is (:authenticated? user-client)))
          (testing "We can index the users with the superuser-authenticated
                  client."
            (let [{:keys [status body]} (client/index-users superuser-client)]
              (is (= 200 status))
              (is (user-specs/users? (:data body)))
              (is (seq (:data body)))
              (let [user-keys (->> body :data (mapcat keys) set)]
                (is (some #{:email} user-keys))
                (is (some #{:is-superuser?} user-keys)))))
          (testing "The user-authenticated client can index the users but receives
                  redacted data."
            (let [{:keys [status body]} (client/index-users user-client)]
              (is (= 200 status))
              (is (user-specs/users? (:data body)))
              (is (seq (:data body)))
              (let [user-keys (->> body :data (mapcat keys) set)]
                (is (not (some #{:email} user-keys)))
                (is (not (some #{:is-superuser?} user-keys))))))
          (testing "User creation fails in the client if the payload is invalid"
            (let [ex (try (client/create-user
                           superuser-client
                           (assoc (gen/generate (s/gen ::user-specs/user-write))
                                  :is-superuser? 8))
                          (catch Exception e (ex-data e)))]
              (is (= :invalid-boolean (:error-code ex)))
              (is (= [:is-superuser] (-> ex :data :path)))))
          (testing "The superuser-authenticated client can create a new user."
            (let [new-user-password "1234"
                  {:keys [status] created-user :body}
                  (client/create-user
                   superuser-client
                   (assoc (gen/generate (s/gen ::user-specs/user-write))
                          :password new-user-password
                          :is-superuser? false))]
              (is (= 201 status))
              (is (uuid? (:id created-user)))
              (is (jt/instant? (:created-at created-user)))
              (is (jt/instant? (:updated-at created-user)))
              (is (nil? (:destroyed-at created-user)))
              (testing "The superuser-authenticated client can fetch the newly
                      created user."
                (let [{fetched-user :body} (client/show-user
                                            superuser-client (:id created-user))]
                  (is (= created-user fetched-user))))
              (testing "The superuser-authenticated client can update the newly
                     created user."
                (let [{updated-user :body} (client/update-user
                                            superuser-client
                                            (:id created-user)
                                            {:first-name "Timothy"})]
                  (is (= (-> created-user
                             (assoc :first-name "Timothy"
                                    :updated-by su-id)
                             (dissoc :updated-at))
                         (dissoc updated-user :updated-at)))
                  (testing "The non-superuser-authenticated client can fetch the
                          newly-created user but receives a redacted version."
                    (let [{:keys [status] user :body}
                          (client/show-user user-client (:id created-user))]
                      (is (= 200 status))
                      (is (user-specs/user? user))
                      (is (not (contains? user :email)))
                      (is (not (contains? user :is-superuser?)))))
                  (testing "The non-superuser-authenticated client cannot update
                          the newly-created user."
                    (let [{:keys [status] error :body}
                          (client/update-user
                           user-client
                           (:id created-user)
                           {:first-name "Danuary"})]
                      (is (= 403 status))
                      (is (= "unauthorized" (-> error :errors first :error-code)))))
                  (testing "It is not possible to authenticate with the
                          newly-created user because it has not yet been
                          activated."
                    (let [{:keys [status] error :body}
                          (client/login
                           (client/make-client :local-test)
                           (:email created-user)
                           new-user-password)]
                      (is (= 401 status))
                      (is (= "unregistered-user" (-> error :errors first :error-code)))))
                  (testing "We can activate the user without authenticating. Note:
                          the registration key would be emailed to the user
                          after initial signup, in the normal course of events."
                    (let [user-from-db (db.users/get-user database (:id created-user))
                          {:keys [status] activated-user :body}
                          (client/activate-user
                           (client/make-client :local-test)
                           (:id created-user)
                           (:registration-key user-from-db))]
                      (is (= 200 status))
                      (is (= :registered (:registration-status activated-user)))
                      (is (= :pending (:registration-status updated-user)))
                      (is (= (dissoc updated-user :updated-at :registration-status)
                             (dissoc activated-user :updated-at :registration-status)))))
                  (testing "It is possible to authenticate with the newly-activated user."
                    (let [new-client (client/authenticate-client
                                      (client/make-client :local-test)
                                      (:email created-user)
                                      new-user-password)]
                      (is (:authenticated? new-client))
                      (testing "User deletion."
                        (testing "User deletion attempts return 404."
                          (let [{:keys [status] error :body} (client/delete-user
                                                              new-client (:id updated-user))]
                            (is (= 404 status))
                            (is (= "unrecognized-operation"
                                   (-> error :errors first :error-code))))))
                      (testing "A non-superuser cannot deactivate another user."
                        (let [{:keys [status] error :body}
                              (client/deactivate-user user-client
                                                      (:id updated-user))]
                          (is (= 403 status))
                          (is (= "unauthorized" (-> error :errors first :error-code)))))
                      (testing "The new user can deactivate itself."
                        (let [{:keys [status] deactivated-user :body}
                              (client/deactivate-user new-client
                                                      (:id updated-user))]
                          (is (= 200 status))
                          (is (user-specs/user? deactivated-user))
                          (is (= :deactivated (:registration-status deactivated-user))))))))))
            ;; Unauthenticated client can bootstrap: create a new (non-superuser) user.
            ;; See the bootstrap-end-to-end test below for more in this vein
            (testing "An unauthenticated client can create a new user."
              (let [new-user-password "1234"
                    unauthenticated-client (client/make-client :local-test)
                    {:keys [status] created-user :body}
                    (client/create-user
                     unauthenticated-client
                     (assoc (gen/generate (s/gen ::user-specs/user-write))
                            :password new-user-password
                            :is-superuser? false))]
                (is (= 201 status))
                (is (user-specs/user? created-user))))
            (testing "An unauthenticated client canNOT create a new SUPERuser."
              (let [new-user-password "1234"
                    unauthenticated-client (client/make-client :local-test)
                    {:keys [status] error :body}
                    (client/create-user
                     unauthenticated-client
                     (assoc (gen/generate (s/gen ::user-specs/user-write))
                            :password new-user-password
                            :is-superuser? true))]
                (is (= 403 status))
                (is (= "unauthorized" (-> error :errors first :error-code)))))))
        (finally (component/stop system))))))

(deftest user-passwords-can-be-reset
  (let [{:keys [database] :as system} (component/start (u/new-system))]
    (try
      (let [{:keys [user user-password]} (u/setup database)
            {user-email :email user-id :id} user
            unauthenticated-client (client/make-client :local-test)]
        (is (uuid? (:registration-key user)))
        (testing "We can initiate password reset with an unauthenticated client."
          (let [{:keys [status] empty-body :body}
                (client/initiate-password-reset unauthenticated-client user-id)]
            (is (= 204 status))
            (is (nil? empty-body))
            (let [db-user-after (db.users/get-user database user-id)]
              (is (uuid? (:registration-key db-user-after)))
              (is (not= (:registration-key user)
                        (:registration-key db-user-after)))
              (testing "We can initiate password reset again; it'll just refresh the key."
                (let [{:keys [status] empty-body :body}
                      (client/initiate-password-reset unauthenticated-client user-id)]
                  (is (= 204 status))
                  (is (nil? empty-body))
                  (let [db-user-after-2 (db.users/get-user database user-id)]
                    (is (uuid? (:registration-key db-user-after)))
                    (is (not= (:registration-key db-user-after)
                              (:registration-key db-user-after-2)))))))))
        (testing "Password reset fails with non-existent user ID."
          (let [{:keys [status] error :body}
                (client/reset-password unauthenticated-client
                                       (random-uuid)
                                       {:secret-key (random-uuid)
                                        :password "new-password"})]
            (is (= 404 status))
            (is (= "entity-not-found" (u/first-error-code error)))))
        (testing "Password reset fails bad secret key"
          (let [{:keys [status] error :body}
                (client/reset-password unauthenticated-client
                                       user-id
                                       {:secret-key (random-uuid)
                                        :password "new-password"})]
            (is (= 400 status))
            (is (= "secret-key-invalid" (u/first-error-code error)))))
        (testing "We can currently authenticate with the original password"
          (let [authenticated-client
                (client/authenticate-client
                 (client/make-client :local-test)
                 user-email user-password)]
            (is (:authenticated? authenticated-client))))
        ;; WARNING: In production, the secret key would be emailed to the user.
        (testing "Password can be reset with the correct secret key"
          (let [db-user (db.users/get-user database user-id)
                {:keys [status] user :body}
                (client/reset-password
                 unauthenticated-client
                 user-id
                 {:secret-key (:registration-key db-user)
                  :password "new-password"})]
            (is (= 200 status))
            (is (user-specs/user? user))
            (testing "We can authenticate with the updated password"
              (let [authenticated-client
                    (client/authenticate-client
                     (client/make-client :local-test)
                     user-email "new-password")]
                (is (:authenticated? authenticated-client))))
            (testing "We can no longer authenticate with the original password"
              (let [authenticated-client
                    (client/authenticate-client
                     (client/make-client :local-test)
                     user-email user-password)]
                (is (not (:authenticated? authenticated-client))))))))
      (finally (component/stop system)))))

(def test-clock
  (clock/map->Clock
   {:now-fn (constantly (jt/plus (jt/instant) (jt/days 2)))}))

(deftest rate-limiting-on-user-creation-works-end-to-end
  (let [ip-1 (u/gen-ip)
        ip-2 (u/gen-ip)
        unauthenticated-client (client/make-client :local-test)]
    (testing "Rate limiting works"
      (let [{:keys [database] :as system} (component/start (u/new-system))]
        (bond/with-stub [[utils/remote-addr (constantly ip-1)]]
          (try
            (let [_unused (u/setup database)]
              (testing "An unauthenticated client can create a new user."
                (let [new-user-password "1234"
                      {:keys [status] created-user :body}
                      (client/create-user
                       unauthenticated-client
                       (assoc (gen/generate (s/gen ::user-specs/user-write))
                              :password new-user-password
                              :is-superuser? false))]
                  (is (= 201 status))
                  (is (user-specs/user? created-user))))
              (testing "Attempting to use an unauthenticated client to create another new user immediately after will fail due to rate limiting."
                (let [new-user-password "2222"
                      {:keys [status] error :body}
                      (client/create-user
                       unauthenticated-client
                       (assoc (gen/generate (s/gen ::user-specs/user-write))
                              :password new-user-password
                              :is-superuser? false))]
                  (is (= 429 status))
                  (is (= "too-many-requests" (u/first-error-code error)))))
              (testing "If the request to unauthenticatedly create a new user comes from a novel IP, it works."
                (bond/with-stub [[utils/remote-addr (constantly ip-2)]]
                  (let [new-user-password "9999"
                        {:keys [status] created-user :body}
                        (client/create-user
                         unauthenticated-client
                         (assoc (gen/generate (s/gen ::user-specs/user-write))
                                :password new-user-password
                                :is-superuser? false))]
                    (is (= 201 status))
                    (is (user-specs/user? created-user))))))
            (finally (component/stop system))))))
    (testing "Rate limiting does not apply after enough time has passed"
      (let [system (component/start (assoc (u/new-system) :clock test-clock))]
        (bond/with-stub [[utils/remote-addr (constantly ip-1)]]
          (try
            (testing "An unauthenticated client can create a new user."
              (let [new-user-password "1234"
                    {:keys [status] created-user :body}
                    (client/create-user
                     unauthenticated-client
                     (assoc (gen/generate (s/gen ::user-specs/user-write))
                            :password new-user-password
                            :is-superuser? false))]
                (is (= 201 status))
                (is (user-specs/user? created-user))))
            (finally (component/stop system))))))))
