(ns dvb.server.db.users-plans-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [dvb.common.specs.users-plans :as users-plans-specs]
            [dvb.server.db.plans :as plans]
            [dvb.server.db.users-plans :as sut]
            [dvb.server.test-data :as test-data]))

(deftest crud-on-users-plans-works
  (let [{:keys [user database]} (test-data/set-up-old-user)
        {user-id :id} user
        {:as created-plan plan-id :id}
        (plans/create-plan database
                           {:tier :free
                            :created-by user-id
                            :updated-by user-id})]
    (try
      (testing "We can create a new user plan."
        (let [user-plan (sut/create-user-plan
                         database
                         {:user-id user-id
                          :plan-id plan-id
                          :role :manager
                          :created-by user-id
                          :updated-by user-id})]
          (is (users-plans-specs/user-plan? user-plan))
          (is (= (:updated-at user-plan) (:created-at user-plan)))
          (is (nil? (:destroyed-at user-plan)))
          (testing "We can query our newly created user plan."
            (let [selected-user-plan (sut/get-user-plan database (:id user-plan))]
              (is (= user-plan selected-user-plan))))
          (testing "We can update the user plan."
            (let [updated-user-plan
                  (sut/update-user-plan
                   database
                   (assoc user-plan :role :member))]
              (is (= (-> user-plan
                         (assoc :role :member)
                         (dissoc :updated-at))
                     (dissoc updated-user-plan :updated-at)))
              (testing "We can (soft) delete the user plan."
                (let [deleted-user-plan (sut/delete-user-plan database user-plan)]
                  (is (= (dissoc updated-user-plan :updated-at :destroyed-at)
                         (dissoc deleted-user-plan :updated-at :destroyed-at)))
                  (is (:destroyed-at deleted-user-plan))
                  (is (nil? (sut/get-user-plan database (:id user-plan))))))))
          (testing "We can view the history of the user plan."
            (let [history (sut/get-history database (:id user-plan))
                  history-summary
                  (->> history
                       (map (fn [event] (-> event :row-data :role)))
                       reverse)]
              (is (= [:manager :member :member] history-summary))))))
      (finally (component/stop database)))))
