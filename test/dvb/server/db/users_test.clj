(ns dvb.server.db.users-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [dvb.common.specs.users :as users-specs]
            [dvb.server.db.users :as sut]
            [dvb.server.test-data :as test-data]))

(deftest crud-on-users-works
  (let [{:keys [user database]} (test-data/set-up-old-user)]
    (try
      (testing "We can create a new user."
        (is (users-specs/user? user))
        (is (uuid? (:id user)))
        (is (inst? (:created-at user)))
        (is (inst? (:updated-at user)))
        (is (= (:updated-at user) (:created-at user)))
        (is (nil? (:destroyed-at user)))
        (testing "We can query our newly created user."
          (let [selected-user (sut/get-user database (:id user))]
            (is (= user selected-user))))
        (testing "We can update the user."
          (let [updated-user (sut/update-user database (assoc user :last-name "Robson"))]
            (is (= (-> user (assoc :last-name "Robson") (dissoc :updated-at))
                   (dissoc updated-user :updated-at)))
            (testing "We can (soft) delete the user."
              (let [deleted-user (sut/delete-user database user)]
                (is (= (dissoc updated-user :updated-at :destroyed-at)
                       (dissoc deleted-user :updated-at :destroyed-at)))
                (is (:destroyed-at deleted-user))
                (is (nil? (sut/get-user database (:id user))))))))
        (testing "We can view the history of the user."
          (let [history (sut/get-history database (:id user))
                history-summary
                (->> history
                     (map (fn [event] {:last-name (-> event :row-data :last-name)
                                       :active? (-> event :row-data :destroyed-at nil?)}))
                     reverse)
                last-name-history (->> history-summary (map :last-name) rest)
                active-history (->> history-summary (map :active?))]
            (is (= ["Robson" "Robson"] last-name-history))
            (is (= [true true false] active-history)))))
      (finally (component/stop database)))))
