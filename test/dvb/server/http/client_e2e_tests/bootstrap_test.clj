(ns dvb.server.http.client-e2e-tests.bootstrap-test
  "Bootstrap AKA Sign-up tests. We can create a free account and get started
  with creating users and OLDs right away."
  (:require [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [dvb.client.core :as client]
            [dvb.common.specs.olds :as old-specs]
            [dvb.common.specs.plans :as plan-specs]
            [dvb.common.specs.users :as user-specs]
            [dvb.common.specs.user-olds :as user-old-specs]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.client-e2e-tests.utils :as u]))

(deftest bootstrap-end-to-end
  (let [{:keys [database] :as system} (component/start (u/new-system))]
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
                  {user-with-plans :body} (client/show-user client user-id
                                                            {:include-plans? true})
                  {plan-with-members :body} (client/show-plan client
                                                              plan-id
                                                              {:include-members? true})
                  {{:as old old-slug :slug} :body} (client/create-old ;
                                                    client {:plan-id plan-id})
                  ;; Following is useful for pprinting
                  _summary {:user (-> user-with-plans
                                      (select-keys [:id :email :is-superuser? :plans])
                                      (assoc :password user-password))
                           :plan (-> plan-with-members
                                     (select-keys [:id :tier :members]))
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
