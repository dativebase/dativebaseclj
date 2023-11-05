(ns old.db.forms-test
  (:require [clojure.test :as t]
            [com.stuartsierra.component :as component]
            old.db.core
            [old.db.olds :as olds]
            [old.db.test-queries :as test-queries]
            [old.db.forms :as sut]
            [old.db.users :as users]
            [old.system.db :as system-db]))

(defn- set-up
  "Given a clean slate, create OLD lan-old and user Alice! Then return
   db-component, db-conn, user and old."
  []
  (let [db-component (component/start (system-db/map->DB {:db-name :old}))
        db-conn (system-db/conn db-component)]
    (test-queries/delete-all-the-things db-conn)
    {:old (olds/create-old db-conn {:slug "lan-old"
                                    :name "Language"})
     :user (users/create-user db-conn
                              {:first-name "Alice"
                               :last-name "Bobson"
                               :email "ab@hmail.com"
                               :username "ab"
                               :password "ENCRYPTME!!!"})
     :db-conn db-conn
     :db-component db-component}))

(t/deftest forms-can-be-created-read-updated-deleted
  (let [{:keys [old user db-conn db-component]} (set-up)]
    (try
      (t/testing "We can create a form."
        (let [form (sut/create-form db-conn
                                    {:old-slug (:slug old)
                                     :created-by-user-id (:id user)
                                     :transcription "Да"})]
          (t/is (= (:id user) (:created-by-user-id form)))
          (t/is (= (:slug old) (:old-slug form)))
          (t/is (uuid? (:id form)))
          (t/is (= (:inserted-at form)
                   (:created-at form)
                   (:updated-at form)))
          (t/is (nil? (:destroyed-at form)))
          (t/is (= "Да" (:transcription form)))
          (t/testing "We can query our newly created form."
            (let [selected-form (sut/get-form db-conn (:id form))]
              (t/is (= form selected-form))))
          (t/testing "We can update the form."
            (let [updated-form (sut/update-form db-conn (assoc form :transcription "До"))]
              (t/is (= (-> form (assoc :transcription "До") (dissoc :updated-at))
                       (dissoc updated-form :updated-at)))
              (t/testing "We can (soft) delete the form."
                (let [deleted-form (sut/delete-form db-conn form)]
                  (t/is (= (dissoc updated-form :updated-at :destroyed-at)
                           (dissoc deleted-form :updated-at :destroyed-at)))
                  (t/is (:destroyed-at deleted-form))
                  (t/is (nil? (sut/get-form db-conn (:id form))))))))
          (t/testing "We can view the history of the form."
            (let [history (sut/get-history db-conn form)]
              (t/is (= [{:transcription "Да" :active? true}
                        {:transcription "До" :active? true}
                        {:transcription "До" :active? false}]
                       (->> history
                            (map (fn [event] {:transcription (-> event :row-data :transcription)
                                              :active? (-> event :row-data :destroyed-at nil?)}))
                            reverse)))))))
      (finally (component/stop db-component)))))

(t/deftest a-set-of-ordered-forms-can-be-selected
  (let [{:keys [old user db-conn db-component]} (set-up)]
    (try
      (let [base-form {:old-slug (:slug old) :created-by-user-id (:id user)}]
        (sut/create-form db-conn (assoc base-form :transcription "a"))
        (sut/create-form db-conn (assoc base-form :transcription "b"))
        (sut/create-form db-conn (assoc base-form :transcription "c"))
        (sut/create-form db-conn (assoc base-form :transcription "d"))
        (sut/create-form db-conn (assoc base-form :transcription "e"))
        (t/is (= ["a" "b" "c" "d" "e"]
                 (map :transcription (sut/get-forms db-conn (:slug old)))))
        (t/is (= ["a" "b" "c"]
                 (map :transcription (sut/get-forms db-conn (:slug old) 3))))
        (t/is (= ["c" "d"]
                 (map :transcription (sut/get-forms db-conn (:slug old) 2 2)))))
      (finally (component/stop db-component)))))
