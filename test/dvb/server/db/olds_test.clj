(ns dvb.server.db.olds-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.stuartsierra.component :as component]
            [dvb.common.specs.olds :as olds-specs]
            [dvb.server.db.olds :as sut]
            [dvb.server.db.users :as users]
            [dvb.server.test-data :as test-data]))

(defn- set-up []
  (let [database (test-data/db-component)]
    {:user (users/create-user database (test-data/gen-user-write
                                        {:created-by nil
                                         :updated-by nil}))
     :database database}))

(deftest old-db-fns-work
  (let [{:keys [database user]} (set-up)
        {user-id :id} user]
    (try
      (testing "create-old works"
        (let [old (sut/create-old database
                                  (test-data/gen-old-write
                                   {:created-by user-id
                                    :updated-by user-id
                                    :plan-id nil}))]
          (is (olds-specs/old? old))
          (is (= (:updated-at old) (:created-at old)))
          (is (nil? (:destroyed-at old)))
          (testing "get-old works"
            (let [selected-old (sut/get-old database (:slug old))]
              (is (= old selected-old))))
          (testing "update-old works"
            (let [plan-id (random-uuid)
                  updated-old (sut/update-old database
                                              (assoc old
                                                     :name "Siksika"
                                                     :plan-id plan-id))]
              (is (= (-> old
                         (assoc :name "Siksika"
                                :plan-id plan-id)
                         (dissoc :updated-at))
                     (dissoc updated-old :updated-at)))
              (testing "delete-old works"
                (let [deleted-old (sut/delete-old database old)]
                  (is (= (dissoc updated-old :updated-at :destroyed-at)
                         (dissoc deleted-old :updated-at :destroyed-at)))
                  (is (:destroyed-at deleted-old))
                  (is (nil? (sut/get-old database (:slug old))))))))
          (testing "OLD history works"
            (let [history (sut/get-history database (:slug old))
                  focused-history
                  (mapv (fn [event]
                          (select-keys (:row-data event)
                                       [:name :updated-at :destroyed-at]))
                        history)]
              (is (= ["Siksika" "Siksika"]
                     (take 2 (map :name focused-history))))
              (is (= [false true true]
                     (map (comp nil? :destroyed-at) focused-history)))))))
      (finally (component/stop database)))))
