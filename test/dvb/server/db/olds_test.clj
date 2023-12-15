(ns dvb.server.db.olds-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.stuartsierra.component :as component]
            dvb.server.db.core
            [dvb.server.db.olds :as sut]
            [dvb.server.db.test-queries :as test-queries]
            [dvb.server.system.db :as system-db]))

(deftest old-db-fns-work
  (let [database (component/start
                  (system-db/make-db
                   {:name "dativebase"
                    :user "postgres"
                    :password ""}))]
    (try
      (test-queries/delete-all-the-things database)
      (testing "create-old works"
        (let [old (sut/create-old database {:slug "bla"
                                            :name "Blackfoot"})]
          (is (= ["bla" "Blackfoot"] ((juxt :slug :name) old)))
          (is (inst? (:created-at old)))
          (is (inst? (:updated-at old)))
          (is (= (:updated-at old) (:created-at old)))
          (is (nil? (:destroyed-at old)))
          (testing "get-old works"
            (let [selected-old (sut/get-old database (:slug old))]
              (is (= old selected-old))))
          (testing "update-old works"
            (let [updated-old (sut/update-old database (assoc old :name "Siksika"))]
              (is (= (-> old (assoc :name "Siksika") (dissoc :updated-at))
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
              (is (= ["Siksika" "Siksika" "Blackfoot"]
                     (map :name focused-history)))
              (is (= [false true true]
                     (map (comp nil? :destroyed-at) focused-history)))))))
      (finally (component/stop database)))))
