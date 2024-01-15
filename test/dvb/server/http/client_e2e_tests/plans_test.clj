(ns dvb.server.http.client-e2e-tests.plans-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [dvb.client.core :as client]
            [dvb.common.specs.olds :as old-specs]
            [dvb.common.specs.plans :as plan-specs]
            [dvb.common.specs.users :as user-specs]
            [dvb.common.specs.user-plans :as user-plan-specs]
            [dvb.common.specs.user-olds :as user-old-specs]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.client-e2e-tests.utils :as u]
            [dvb.server.test-data :as test-data]))

(deftest plans-endpoints-work-end-to-end
  (let [{:keys [database] :as system} (component/start (u/new-system))]
    (try
      (let [{:keys [user user-password superuser]} (u/setup database)
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
        (testing "Plan creation fails in the client if the payload is invalid"
          (let [ex (try (client/create-plan client {:tier :gorgonzola-tier})
                        (catch Exception e (ex-data e)))]
            (is (= :invalid-given-enum (:error-code ex)))
            (is (= [:tier] (-> ex :data :path)))))
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
            (testing "Our second user cannot make itself a manager of the plan because it is not authorized because it is neither a superuser nor a manager of the plan."
              (let [{:keys [status] error :body}
                    (client/create-user-plan
                     client-2
                     {:user-id (:id user-2)
                      :plan-id plan-id
                      :role :manager})]
                (is (= 403 status))
                (is (= "unauthorized" (-> error :errors first :error-code)))))
            (testing "The original user can make the second user a member of the plan that the original user created (and now manages)."
              (let [{:keys [status] created-user-plan-2 :body}
                    (client/create-user-plan
                     client
                     {:user-id (:id user-2)
                      :plan-id plan-id
                      :role :member})]
                (is (= 201 status))
                (is (user-plan-specs/user-plan? created-user-plan-2))
                (testing "The (non-manager) member of the plan is not authorized to upgrade itself to manager."
                  (let [{:keys [status] error :body}
                        (client/update-user-plan
                         client-2
                         (:id created-user-plan-2)
                         (assoc created-user-plan-2 :role :manager))]
                    (is (= 403 status))
                    (is (= "unauthorized" (-> error :errors first :error-code)))))
                (testing "The original user can upgrade the second user from member to manager of the plan that the original user created (and manages)."
                  (let [{:keys [status] updated-user-plan-2 :body}
                        (client/update-user-plan
                         client
                         (:id created-user-plan-2)
                         (assoc created-user-plan-2 :role :manager))]
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
                                 (:plans user-with-plans))))))
                ;; NOTE: There is no plan update operation on purpose.
                ;; Plans will be updated when billing events occur.
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
                    (testing "We can remove the second user's access to the plan by deleting the correct user plan."
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
                                         (:members plan-with-members))))))))))
              (testing "We can create an OLD running under our plan."
                (let [{{:as old old-slug :slug} :body}
                      (client/create-old client
                                         (assoc (test-data/gen-old-write)
                                                :plan-id plan-id))]
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
                        (testing "We cannot downgrade the user to a contributor on the OLD because that would leave the OLD without an administrator."
                          (let [{:keys [status] error :body}
                                (client/update-user-old
                                 client user-old-id {:role :contributor})]
                            (is (= 400 status))
                            (is (= "role-transition-violation"
                                   (u/first-error-code error)))
                            (is (= "no-administratorless-olds"
                                   (u/first-data-error-code error)))))
                        (testing "We can make user 2 an administrator for this OLD."
                          (let [{:keys [status] user-old :body}
                                (client/create-user-old
                                 client {:role :administrator
                                         :user-id (:id user-2)
                                         :old-slug old-slug})]
                            (is (= 201 status))
                            (is (user-old-specs/user-old? user-old))))
                        (testing "We can now downgrade the user to a contributor on the OLD because we have created a new administrator."
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
                            (is (not (:authenticated? new-client))))))))))))))
      (finally (component/stop system)))))
