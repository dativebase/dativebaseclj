(ns dvb.server.db.forms-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.stuartsierra.component :as component]
            [dvb.server.db.forms :as sut]
            [dvb.server.test-data :as test-data]))

(deftest crud-on-forms-works
  (let [{:keys [old user database]} (test-data/set-up-old-user)]
    (try
      (testing "We can create a form."
        (let [form (sut/create-form database
                                    {:old-slug (:slug old)
                                     :transcription "Да"
                                     :created-by (:id user)
                                     :updated-by (:id user)})]
          (is (= (:id user) (:created-by form)))
          (is (= (:slug old) (:old-slug form)))
          (is (uuid? (:id form)))
          (is (= (:inserted-at form)
                 (:created-at form)
                 (:updated-at form)))
          (is (nil? (:destroyed-at form)))
          (is (= "Да" (:transcription form)))
          (testing "We can query our newly created form."
            (let [selected-form (sut/get-form database (:id form))]
              (is (= form selected-form))))
          (testing "We can update the form."
            (let [updated-form (sut/update-form database (assoc form :transcription "До"))]
              (is (= (-> form (assoc :transcription "До") (dissoc :updated-at))
                     (dissoc updated-form :updated-at)))
              (testing "We can (soft) delete the form."
                (let [deleted-form (sut/delete-form database form)]
                  (is (= (dissoc updated-form :updated-at :destroyed-at)
                         (dissoc deleted-form :updated-at :destroyed-at)))
                  (is (:destroyed-at deleted-form))
                  (is (nil? (sut/get-form database (:id form))))))))
          (testing "We can view the history of the form."
            (let [history (sut/get-history database form)]
              (is (= [{:transcription "Да" :active? true}
                      {:transcription "До" :active? true}
                      {:transcription "До" :active? false}]
                     (->> history
                          (map (fn [event] {:transcription (-> event :row-data :transcription)
                                            :active? (-> event :row-data :destroyed-at nil?)}))
                          reverse)))))))
      (finally (component/stop database)))))

(deftest a-set-of-ordered-forms-can-be-selected
  (let [{:keys [old user database]} (test-data/set-up-old-user)]
    (try
      (let [base-form {:old-slug (:slug old)
                       :created-by (:id user)
                       :updated-by (:id user)}]
        (sut/create-form database (assoc base-form :transcription "a"))
        (sut/create-form database (assoc base-form :transcription "b"))
        (sut/create-form database (assoc base-form :transcription "c"))
        (sut/create-form database (assoc base-form :transcription "d"))
        (sut/create-form database (assoc base-form :transcription "e"))
        (is (= ["a" "b" "c" "d" "e"]
               (map :transcription (sut/get-forms database (:slug old)))))
        (is (= ["a" "b" "c"]
               (map :transcription (sut/get-forms database (:slug old) 3))))
        (is (= ["c" "d"]
               (map :transcription (sut/get-forms database (:slug old) 2 2)))))
      (finally (component/stop database)))))
