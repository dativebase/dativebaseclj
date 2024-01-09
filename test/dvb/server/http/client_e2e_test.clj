(ns dvb.server.http.client-e2e-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [dvb.client.core :as client]
            [dvb.common.specs.olds :as old-specs]
            [dvb.common.specs.plans :as plan-specs]
            [dvb.common.specs.users :as user-specs]
            [dvb.common.specs.user-plans :as user-plan-specs]
            [dvb.common.specs.user-olds :as user-old-specs]
            [dvb.server.core :as core]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.users :as db.users]
            [dvb.server.db.user-olds :as db.user-olds]
            [dvb.server.system.config :as config]
            [dvb.server.test-data :as test-data]
            [java-time.api :as jt]))

(defn new-system []
  (core/make-main-system (assoc (config/init config/dev-config-path)
                                :server-port 8087)))

(defn setup [database]
  (let [{su-pwd :password :as superuser*}
        (test-data/gen-user-write {:is-superuser? true
                                   :created-by nil
                                   :updated-by nil})
        {su-id :id :as superuser} (db.users/activate-user
                                   database
                                   (db.users/create-user database superuser*))
        provenance {:created-by su-id :updated-by su-id}
        {u-pwd :password :as user*}
        (test-data/gen-user-write (assoc provenance :is-superuser? false))
        user (db.users/activate-user
              database
              (db.users/create-user database user*))
        {old-slug :slug :as old*} (assoc (test-data/gen-old-write provenance)
                                         :plan-id nil)
        old (db.olds/create-old database old*)]
    (db.user-olds/create-user-old
     database (merge provenance {:user-id (:id user)
                                 :old-slug old-slug
                                 :role "contributor"}))
    (db.user-olds/create-user-old
     database (merge provenance {:user-id su-id
                                 :old-slug old-slug
                                 :role "contributor"}))
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
(deftest users-endpoints-work-end-to-end
  (let [{:keys [database] :as system} (component/start (new-system))]
    (try
      (let [{:keys [user user-password superuser superuser-password]}
            (setup database)
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
        (testing "The superuser-authenticated client can create a new user."
          (let [new-user-password "1234"
                {:keys [status] created-user :body}
                (client/create-user superuser-client
                                    {:password new-user-password
                                     :is-superuser? false})]
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
                  (client/create-user unauthenticated-client
                                      {:password new-user-password
                                       :is-superuser? false})]
              (is (= 201 status))
              (is (user-specs/user? created-user))))
          (testing "An unauthenticated client canNOT create a new SUPERuser."
            (let [new-user-password "1234"
                  unauthenticated-client (client/make-client :local-test)
                  {:keys [status] error :body}
                  (client/create-user unauthenticated-client
                                      {:password new-user-password
                                       :is-superuser? true})]
              (is (= 403 status))
              (is (= "unauthorized" (-> error :errors first :error-code)))))))
      (finally (component/stop system)))))

;; Bootstrap AKA Sign-up
;; We can create a free account and get started with creating users and OLDs
;; right away.
(deftest bootstrap-end-to-end
  (let [{:keys [database] :as system} (component/start (new-system))]
    (try
      (testing "An unauthenticated client can create a new user."
        (let [user-password "1234"
              unauthenticated-client (client/make-client :local-test)
              {:keys [status]
               {:as created-user
                user-id :id
                user-email :email
                :keys [created-by is-superuser?]} :body}
              (client/create-user unauthenticated-client
                                  {:password user-password
                                   :is-superuser? false})]
          (is (= 201 status))
          (is (user-specs/user? created-user))
          (is (nil? created-by))
          (is (false? is-superuser?))
          ;; WARNING: In the real world, the activation key would be emailed to
          ;; the user as a link they can simply visit.
          ;; TODO: Given the above, GET /activate-user should
          ;; probably, ultimately, redirect to Dative, or some other
          ;; human-readable HTML or SPA.
          (testing "The unauthenticated client can activate the new user."
            (let [{:as _user-from-db :keys [registration-key]}
                  (db.users/get-user database user-id)
                  {:keys [status] {:as activated-user} :body}
                  (client/activate-user
                   unauthenticated-client
                   user-id
                   registration-key)]
              (is (= 200 status))
              (is (user-specs/user? activated-user))))
          (testing "We can make an authenticated client with the now-registered
                    user and use it to make the user a manager of a new free
                    plan, and create a new OLD, running under that plan."
            (let [client (client/authenticate-client
                          (client/make-client :local-test)
                          user-email
                          user-password)
                  {{:as plan plan-id :id} :body}
                  (client/create-plan client {:tier :free})
                  {user-plan :body} (client/create-user-plan
                                     client {:user-id user-id
                                             :plan-id plan-id
                                             :role :manager})
                  {user-with-plans :body} (client/show-user client user-id
                                                            {:include-plans? true})
                  {plan-with-members :body} (client/show-plan client
                                                              plan-id
                                                              {:include-members? true})
                  {{:as old old-slug :slug} :body} (client/create-old
                                                    client {:plan-id plan-id})
                  ;; Following is useful for pprinting
                  _summary {:user
                           (-> user-with-plans
                               (select-keys [:id :email :is-superuser? :plans])
                               (assoc :password user-password))
                           :plan (-> plan-with-members
                                     (select-keys [:id :tier :members]))
                           :user-plan
                           (-> user-plan
                               (select-keys [:id :role :plan-id :user-id]))
                           :old (-> old
                                    (select-keys [:slug :name :plan-id :users]))}]
              (is (plan-specs/plan? plan))
              (is (plan-specs/plan? plan-with-members))
              (is (user-specs/user? user-with-plans))
              (is (old-specs/old? old))
              (is (= [{:id user-id
                       :role :administrator}]
                     (mapv (fn [u] (select-keys u [:id :role]))
                           (:users old))))
              (testing "We can create a new user and give it access to our OLD."
                (let [additional-user-password "5678"
                      {:keys [status]
                       {:as additional-user
                        additional-user-id :id} :body}
                      (client/create-user client
                                          {:password additional-user-password
                                           :is-superuser? false})]
                  (is (= 201 status))
                  (is (user-specs/user? additional-user))
                  (testing "We can make the new user a contributor in the new OLD."
                    (let [{:keys [status] {:as user-old} :body}
                          (client/create-user-old
                           client
                           {:user-id additional-user-id
                            :old-slug old-slug
                            :role :contributor})]
                      (is (= 201 status))
                      (is (user-old-specs/user-old? user-old))
                      (let [{old-with-users :body}
                            (client/show-old client old-slug
                                             {:include-users? true})]
                        (is (= #{{:id user-id
                                  :role :administrator}
                                 {:id additional-user-id
                                  :role :contributor}}
                               (->> old-with-users
                                    :users
                                    (map (fn [u] (select-keys u [:id :role])))
                                    set))))))))))))
      (finally (component/stop system)))))

;; End-to-end test that verifies:
;; - POST plans
;; - GET plans/:id
;; - DELETE plans/:id
;; - POST user-plans
(deftest plans-endpoints-work-end-to-end
  (let [{:keys [database] :as system} (component/start (new-system))]
    (try
      (let [{:keys [user user-password superuser]} (setup database)
            {u2-pwd :password :as user-2*}
            (test-data/gen-user-write
             {:created-by (:id superuser)
              :updated-by (:id superuser)
              :is-superuser? false})
            user-2 (db.users/activate-user
                    database
                    (db.users/create-user database user-2*))
            user-email (:email user)
            client (client/authenticate-client
                    (client/make-client :local-test)
                    user-email user-password)
            client-2 (client/authenticate-client
                      (client/make-client :local-test)
                      (:email user-2) u2-pwd)]
        (testing "We can create a new plan."
          (let [{:keys [status] {:as created-plan plan-id :id} :body}
                (client/create-plan client {:tier :free})]
            (is (= 201 status))
            (is (nil? (:destroyed-at created-plan)))
            (is (= :free (:tier created-plan)))
            (is (plan-specs/plan? created-plan))
            (let [user-member (->> created-plan
                                   :members
                                   (filter (comp (partial = (:id user)) :id))
                                   first)]
              (is (some? user-member))
              (is (= :manager (:role user-member))))
            (testing "We cannot create a second plan with the same user: one plan per user."
              (let [{:keys [status] error :body}
                    (client/create-plan client {:tier :free})]
                (is (= 403 status))
                (is (= "unauthorized" (-> error :errors first :error-code)))))
            (testing "We can fetch the newly created plan."
              (let [{fetched-plan :body}
                    (client/show-plan client plan-id)]
                (is (= (dissoc created-plan :members) fetched-plan))))
            (testing "A DB uniqueness constraint prevents us from trying to make
                      our client's user a manager of this plan again."
              ;; (Note: our client is automatically made a manager during plan creation.)
              (let [{:keys [status] error :body}
                    (client/create-user-plan
                     client
                     {:user-id (:id user)
                      :plan-id plan-id
                      :role :manager})]
                (is (= 400 status))
                (is (= "users-plans-unique-constraint-violated"
                       (-> error :errors first :error-code)))))
            (testing "Our second user cannot make itself a manager of the plan
                      because it is not authorized (not a superuser, the creator
                      of the plan or a manager of the plan)."
              (let [{:keys [status] error :body}
                    (client/create-user-plan
                     client-2
                     {:user-id (:id user-2)
                      :plan-id plan-id
                      :role :manager})]
                (is (= 403 status))
                (is (= "unauthorized" (-> error :errors first :error-code)))))
            (testing "The original user can make the second user a member of the
                      plan that the original user created (and now manages)."
              (let [{:keys [status] created-user-plan-2 :body}
                    (client/create-user-plan
                     client
                     {:user-id (:id user-2)
                      :plan-id plan-id
                      :role :member})]
                (is (= 201 status))
                (is (user-plan-specs/user-plan? created-user-plan-2))
                (testing "The (non-manager) member of the plan is not authorized
                          to upgrade itself to manager."
                  (let [{:keys [status] error :body}
                        (client/update-user-plan
                         client-2
                         (:id created-user-plan-2)
                         {:role :manager})]
                    (is (= 403 status))
                    (is (= "unauthorized" (-> error :errors first :error-code)))))
                (testing "The original user can upgrade the second user from
                          member to manager of the plan that the original user
                          created (and manages)."
                  (let [{:keys [status] updated-user-plan-2 :body}
                        (client/update-user-plan
                         client
                         (:id created-user-plan-2)
                         {:role :manager})]
                    (is (= 200 status))
                    (is (user-plan-specs/user-plan? updated-user-plan-2))))
                (testing "We can fetch our user with its plans."
                  (let [{:keys [status] user-with-plans :body}
                        (client/show-user client
                                          (:id user)
                                          {:include-plans? true})]
                    (is (= 200 status))
                    (is (user-specs/user? user-with-plans))
                    (is (= [{:id plan-id
                             :role :manager
                             :tier :free}]
                           (mapv (fn [p] (dissoc p :user-plan-id))
                                 (:plans user-with-plans))))))
                (testing "We can fetch user 2 with its plans."
                  (let [{:keys [status] user-with-plans :body}
                        (client/show-user client
                                          (:id user-2)
                                          {:include-plans? true})]
                    (is (= 200 status))
                    (is (user-specs/user? user-with-plans))
                    (is (= [{:id plan-id
                             :role :manager
                             :tier :free}]
                           (mapv (fn [p] (dissoc p :user-plan-id))
                                 (:plans user-with-plans))))))))
            ;; NOTE: no update on purpose. Plans will be updated when billing
            ;; events occur.
            (testing "We can fetch our plan with its users."
              (let [{:keys [status] plan-with-members :body}
                    (client/show-plan
                     client plan-id {:include-members? true})]
                (is (= 200 status))
                (is (plan-specs/plan? plan-with-members))
                (is (= #{{:id (:id user)
                          :role :manager}
                         {:id (:id user-2)
                          :role :manager}}
                       (set (map (fn [m] (select-keys m [:id :role]))
                                 (:members plan-with-members)))))
                (testing "We can remove the second user's access to the plan by
                          deleting the correct user plan."
                  (let [user-plan-id (:user-plan-id
                                      (first (for [m (:members plan-with-members)
                                                   :when (= (:id user-2)
                                                            (:id m))] m)))
                        {:keys [status] deleted-user-plan :body}
                        (client/delete-user-plan client user-plan-id)]
                    (is (= 200 status))
                    (is (user-plan-specs/user-plan? deleted-user-plan))))
                (testing "The second user no longer has access to the plan."
                  (let [{:keys [status] plan-with-members :body}
                        (client/show-plan
                         client plan-id {:include-members? true})]
                    (is (= 200 status))
                    (is (plan-specs/plan? plan-with-members))
                    (is (= #{{:id (:id user)
                              :role :manager}}
                           (set (map (fn [m] (select-keys m [:id :role]))
                                     (:members plan-with-members)))))))))
            (testing "We can create an OLD running under our plan."
              (let [{{:as old old-slug :slug} :body}
                    (client/create-old client {:plan-id plan-id})]
                (is (old-specs/old? old))
                (testing "We can view our plan with its members and its OLDs"
                  (let [{:keys [status] plan-with-members-and-olds :body}
                        (client/show-plan client plan-id
                                          {:include-members? true
                                           :include-olds? true})]
                    (is (= 200 status))
                    (is (plan-specs/plan? plan-with-members-and-olds))
                    (is (= [old-slug] (:olds plan-with-members-and-olds)))))
                (testing "Deactivation of the user is prohibited because the
                          user is currently the manager of a plan."
                  (let [{:keys [status] error :body}
                        (client/deactivate-user client (:id user))]
                    (is (= 400 status))
                    (is (= "user-deactivation-failed"
                           (-> error :errors first :error-code)))
                    (is (= "cannot-deactivate-manager"
                           (-> error :errors first :data :error-code)))))
                (testing "We cannot delete the newly-created plan right now because
                          it has OLDs running under it."
                  (let [{:keys [status] error :body}
                        (client/delete-plan client (:id created-plan))]
                    (is (= 400 status))
                    (is (= "plan-with-olds-not-delible"
                           (-> error :errors first :error-code)))))
                (testing "We can stop paying for the OLD under our plan."
                  (let [{:keys [status] updated-old :body}
                        (client/update-old client old-slug {:plan-id nil})]
                    (is (= 200 status))
                    (is (old-specs/old? updated-old))
                    (is (nil? (:plan-id updated-old)))))
                (testing "We can delete the newly-created plan."
                  (let [{deleted-plan :body} (client/delete-plan
                                              client plan-id)
                        samer (fn [u] (dissoc u :updated-at :destroyed-at :members))]
                    (is (= (samer created-plan) (samer deleted-plan)))
                    (is (some? (:destroyed-at deleted-plan)))))
                (testing "Deactivation of the user is prohibited because the
                      user is currently the administrator of an OLD."
                  (let [{:keys [status] error :body}
                        (client/deactivate-user client (:id user))]
                    (is (= 400 status))
                    (is (= "user-deactivation-failed"
                           (-> error :errors first :error-code)))
                    (is (= "cannot-deactivate-administrator"
                           (-> error :errors first :data :error-code)))))
                (testing "We can get our OLD with its members"
                  (let [{old-with-users :body}
                        (client/show-old client old-slug {:include-users? true})]
                    (is (old-specs/old? old-with-users))
                    (let [user-id (:id user)
                          user-old-id (-> (for [u (:users old-with-users)
                                                :when (= user-id (:id u))] u)
                                          first :user-old-id)]
                      (testing "We can downgrade the user to a contributor on the OLD."
                        (let [{:keys [status] updated-user-old :body}
                              (client/update-user-old
                               client user-old-id {:role :contributor})]
                          (is (= 200 status))
                          (is (user-old-specs/user-old? updated-user-old))
                          (is (= :contributor (:role updated-user-old)))))
                      (testing "Deactivation of the user is now allowed."
                        (let [{:keys [status] deactivated-user :body}
                              (client/deactivate-user client user-id)]
                          (is (= 200 status))
                          (is (user-specs/user? deactivated-user))
                          (is (= :deactivated (:registration-status deactivated-user)))))
                      (testing "Authentication now fails with the deactivated user"
                        (let [{:keys [status] error :body}
                              (client/show-user client user-id)]
                          (is (= 401 status))
                          (is (= "unauthenticated" (-> error :errors first :error-code))))
                        (let [new-client (client/authenticate-client
                                          (client/make-client :local-test)
                                          user-email user-password)]
                          (is (not (:authenticated? new-client)))))))))))))
      (finally (component/stop system)))))

(deftest olds-endpoints-work-end-to-end
  (let [{:keys [database] :as system} (component/start (new-system))]
    (try
      (let [{:keys [user user-password]} (setup database)
            user-email (:email user)
            client (client/authenticate-client
                    (client/make-client :local-test)
                    user-email user-password)]
        (testing "We can create a new OLD."
          (let [{:keys [status] {:as created-old old-slug :slug} :body}
                (client/create-old client (test-data/gen-old-write))]
            (is (= 201 status))
            (is (nil? (:destroyed-at created-old)))
            (is (old-specs/old? created-old))
            (testing "We cannot create a new OLD with the same slug that was just used."
              (let [{:keys [status] error :body}
                    (client/create-old client (test-data/gen-old-write
                                               {:slug old-slug}))]
                (is (= 400 status))
                (is (= "unique-slug-constraint-violated"
                       (-> error :errors first :error-code)))))
            (testing "We can fetch the newly created OLD."
              (let [{:keys [status] fetched-old :body}
                    (client/show-old client old-slug {:include-users? true})]
                (is (= 200 status))
                (is (old-specs/old? fetched-old))
                (is (= created-old fetched-old))))
            (testing "We can update the name of the newly created OLD."
              (let [{:keys [status] updated-old :body}
                    (client/update-old client old-slug {:name "Funions"})]
                (is (= 200 status))
                (is (old-specs/old? updated-old))
                (is (= (-> created-old
                           (assoc :name "Funions")
                           (dissoc :updated-at :updated-by :users))
                       (dissoc updated-old :updated-at :updated-by)))
                (testing "We can query all of the OLDs in the system"
                  (let [{:keys [status] {first-page-of-olds :data
                                         :keys [meta]} :body}
                        (client/index-olds client)]
                    (is (= 200 status))
                    (is (old-specs/olds? first-page-of-olds))
                    (is (<= (count first-page-of-olds) (:items-per-page meta)))
                    (is (zero? (:page meta)))))
                (testing "We can delete the newly created OLD."
                  (let [{:keys [status] deleted-old :body}
                        (client/delete-old client old-slug)]
                    (is (= 200 status))
                    (is (old-specs/old? deleted-old))
                    (is (= (-> updated-old
                               (dissoc :updated-at :destroyed-at))
                           (dissoc deleted-old :updated-at :destroyed-at))))))))))
      (finally (component/stop system)))))

;; Starting with just a user, we can user the API to ensure the user has a free
;; plan and an OLD running under it.
(deftest cover-olds-with-plans
  (let [{:keys [database] :as system} (component/start (new-system))]
    (try
      (let [{:keys [user user-password superuser]}
            (setup database)
            {user-email :email user-id :id} user
            {superuser-id :id} superuser
            client (client/authenticate-client
                    (client/make-client :local-test)
                    user-email user-password)
            {user-2-password :password :as user-2*}
            (test-data/gen-user-write
             {:created-by superuser-id
              :updated-by superuser-id
              :is-superuser? false})
            user-2 (db.users/activate-user
                    database (db.users/create-user database user-2*))
            {user-2-email :email user-2-id :id} user-2
            client-2 (client/authenticate-client
                      (client/make-client :local-test)
                      user-2-email user-2-password)]
        (testing "Happy path: user creates plan and OLD paid for by it."
          (let [{plan :body} (client/create-plan client {:tier :free})
                {plan-id :id} plan
                {user-with-plans :body} (client/show-user client user-id
                                                          {:include-plans? true})
                {plan-with-members :body} (client/show-plan client
                                                            plan-id
                                                            {:include-members? true})
                {{:as old old-slug :slug} :body} (client/create-old
                                                  client {:plan-id plan-id})
                summary {:user
                         (-> user-with-plans
                             (select-keys [:id :email :is-superuser? :plans])
                             (assoc :password user-password))
                         :plan (-> plan-with-members
                                   (select-keys [:id :tier :members]))
                         :old (-> old
                                  (select-keys [:slug :name :plan-id]))}]
            (testing "The user is manager of the plan."
              (is (= {:role :manager
                      :id user-id}
                     (-> plan :members first (select-keys [:role :id])))))
            (testing "The OLD is paid for by the plan."
              (is (= {:slug old-slug
                      :plan-id plan-id}
                     (-> summary :old (dissoc :name)))))
            (testing "Sad-ish path: a second user cannot pay for their OLD under
                      a plan not managed by that user."
              (let [{:keys [status] error :body} (client/create-old
                                                  client-2 {:plan-id plan-id})]
                (is (= 403 status))
                (is (= "unauthorized" (-> error :errors first :error-code))))
              (let [{old-2 :body} (client/create-old client-2 {})]
                (is (old-specs/old? old-2))
                (is (nil? (:plan-id old-2)))
                (testing "If a manager of the plan makes user 2 a manager too,
                          then user 2 can cover its OLD with the plan."
                  (client/create-user-plan
                   client {:user-id user-2-id
                           :plan-id plan-id
                           :role :manager})
                  (let [{old-2-slug :slug} old-2
                        {old-2-updated :body} (client/update-old
                                               client-2
                                               old-2-slug
                                               {:plan-id plan-id})]
                    (is (old-specs/old? old-2-updated))
                    (is (= plan-id (:plan-id old-2-updated))))))))))
      (finally (component/stop system)))))
