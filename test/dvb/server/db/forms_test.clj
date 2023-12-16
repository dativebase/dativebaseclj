(ns dvb.server.db.forms-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.stuartsierra.component :as component]
            dvb.server.db.core
            [dvb.server.db.olds :as olds]
            [dvb.server.db.test-queries :as test-queries]
            [dvb.server.db.forms :as sut]
            [dvb.server.db.users :as users]
            [dvb.server.system.db :as system-db]))

(defn- set-up
  "Given a clean slate, create OLD lan-old and user Alice! Then return
   database, user, and old."
  []
  (let [database (component/start (system-db/make-db
                                   {:name "dativebase"
                                    :user "postgres"
                                    :password ""}))]
    (test-queries/delete-all-the-things database)
    {:old (olds/create-old database {:slug "lan-old"
                                     :name "Language"})
     :user (users/create-user database
                              {:first-name "Alice"
                               :last-name "Bobson"
                               :email "ab@hmail.com"
                               :username "ab"
                               :password "ENCRYPTME!!!"})
     :database database}))

(deftest forms-can-be-created-read-updated-deleted
  (let [{:keys [old user database]} (set-up)]
    (try
      (testing "We can create a form."
        (let [form (sut/create-form database
                                    {:old-slug (:slug old)
                                     :created-by (:id user)
                                     :transcription "Да"})]
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
  (let [{:keys [old user database]} (set-up)]
    (try
      (let [base-form {:old-slug (:slug old) :created-by (:id user)}]
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
