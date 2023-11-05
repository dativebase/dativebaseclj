(ns old.db.olds-test
  (:require [clojure.test :as t]
            [com.stuartsierra.component :as component]
            old.db.core
            [old.db.olds :as sut]
            [old.db.test-queries :as test-queries]
            [old.system.db :as system-db]))

(t/deftest old-db-fns-work
  (let [db-component (component/start (system-db/map->DB {:db-name :old}))
        db-conn (system-db/conn db-component)]
    (try
      (test-queries/delete-all-the-things db-conn)
      (t/testing "create-old works"
        (let [old (sut/create-old db-conn {:slug "bla"
                                           :name "Blackfoot"})]
          (t/is (= ["bla" "Blackfoot"] ((juxt :slug :name) old)))
          (t/is (inst? (:created-at old)))
          (t/is (inst? (:updated-at old)))
          (t/is (= (:updated-at old) (:created-at old)))
          (t/is (nil? (:destroyed-at old)))
          (t/testing "get-old works"
            (let [selected-old (sut/get-old db-conn (:slug old))]
              (t/is (= old selected-old))))
          (t/testing "update-old works"
            (let [updated-old (sut/update-old db-conn (assoc old :name "Siksika"))]
              (t/is (= (-> old (assoc :name "Siksika") (dissoc :updated-at))
                       (dissoc updated-old :updated-at)))
              (t/testing "delete-old works"
                (let [deleted-old (sut/delete-old db-conn old)]
                  (t/is (= (dissoc updated-old :updated-at :destroyed-at)
                           (dissoc deleted-old :updated-at :destroyed-at)))
                  (t/is (:destroyed-at deleted-old))
                  (t/is (nil? (sut/get-old db-conn (:slug old))))))))
          (t/testing "OLD history works"
            (let [history (sut/get-history db-conn (:slug old))
                  focused-history
                  (mapv (fn [event]
                          (select-keys (:row-data event)
                                       [:name :updated-at :destroyed-at]))
                        history)]
              (t/is (= ["Siksika" "Siksika" "Blackfoot"]
                       (map :name focused-history)))
              (t/is (= [false true true]
                       (map (comp nil? :destroyed-at) focused-history)))))))
      (finally (component/stop db-component)))))
