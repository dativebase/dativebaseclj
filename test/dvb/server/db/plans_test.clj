(ns dvb.server.db.plans-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [dvb.common.specs.plans :as plans-specs]
            [dvb.server.db.plans :as sut]
            [dvb.server.db.users :as users]
            [dvb.server.test-data :as test-data]
            [java-time.api :as jt]))

(defn- set-up []
  (let [database (test-data/db-component)]
    {:user (users/create-user database (test-data/gen-user-write
                                        {:created-by nil
                                         :updated-by nil}))
     :database database}))

(deftest crud-on-plans-works
  (let [{:keys [user database]} (set-up)
        {user-id :id} user]
    (try
      (testing "We can create a new plan."
        (let [{:as created-plan plan-id :id}
              (sut/create-plan database
                               {:tier :free
                                :created-by user-id
                                :updated-by user-id})]
          (is (= :free (:tier created-plan)))
          (is (uuid? (:id created-plan)))
          (is (uuid? (:created-by created-plan)))
          (is (jt/instant? (:created-at created-plan)))
          (testing "We can fetch the created plan"
            (let [fetched-plan (sut/get-plan database plan-id)]
              (is (= created-plan fetched-plan))))
          (testing "We can update the created plan"
            (let [updated-plan (sut/update-plan
                                database
                                (assoc created-plan :tier :supporter))]
              (is (= (-> created-plan
                         (assoc :tier :supporter)
                         (dissoc :updated-at))
                     (dissoc updated-plan :updated-at)))))
          (testing "We can fetch a collection of plans"
            (let [fetched-plans (sut/get-plans database)]
              (is (plans-specs/plans? fetched-plans))
              (is (< (count fetched-plans) 11))))
          (testing "We can delete the plan"
            (let [deleted-plan (sut/delete-plan database created-plan)]
              (is (= (:id created-plan) (:id deleted-plan)))))
          (testing "The deleted plan can no longer be fetched"
            (let [fetched-plan (sut/get-plan database plan-id)]
              (is (nil? fetched-plan))))))
      (finally (component/stop database)))))
