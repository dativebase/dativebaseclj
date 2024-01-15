(ns dvb.server.http.client-e2e-tests.cover-olds-with-plans-test
  "Tests that verify that, starting with just a user, we can use the API to
  ensure the user has a free plan and an OLD running under it."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [dvb.client.core :as client]
            [dvb.common.specs.olds :as old-specs]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.client-e2e-tests.utils :as u]
            [dvb.server.test-data :as test-data]))

(deftest cover-olds-with-plans
  (let [{:keys [database] :as system} (component/start (u/new-system))]
    (try
      (let [{:keys [user user-password superuser]} (u/setup database)
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
                {{:as old old-slug :slug} :body}
                (client/create-old
                 client (merge (gen/generate (s/gen ::old-specs/old-write))
                               {:plan-id plan-id}))
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
              (let [{:keys [status] error :body}
                    (client/create-old
                     client-2
                     (assoc (gen/generate (s/gen ::old-specs/old-write))
                            :plan-id plan-id))]
                (is (= 403 status))
                (is (= "unauthorized" (-> error :errors first :error-code))))
              (let [{old-2 :body} (client/create-old
                                   client-2
                                   (gen/generate (s/gen ::old-specs/old-write)))]
                (is (old-specs/old? old-2))
                (is (nil? (:plan-id old-2)))
                (testing "If a manager of the plan makes user 2 a manager too,
                          then user 2 can cover its OLD with the plan."
                  (client/create-user-plan
                   client {:user-id user-2-id
                           :plan-id plan-id
                           :role :manager})
                  (let [{old-2-slug :slug} old-2
                        {old-2-updated :body}
                        (client/update-old
                         client-2
                         old-2-slug
                         (assoc old-2 :plan-id plan-id))]
                    (is (old-specs/old? old-2-updated))
                    (is (= plan-id (:plan-id old-2-updated))))))))))
      (finally (component/stop system)))))
