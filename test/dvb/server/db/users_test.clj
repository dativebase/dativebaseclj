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

(deftest user-roles-work
  (let [{:keys [old user database]} (test-data/set-up-old-user)
        {user-id :id} user]
    (try
      (testing "A user's roles are an empty map when there are no roles."
        (let [user-with-roles (sut/get-user-with-roles database (:id user))]
          (is (= user (dissoc user-with-roles :roles)))
          (is (= {} (:roles user-with-roles)))))
      (testing "We can make a user into an administrator of an OLD."
        (let [user-old (sut/create-user-old database
                                            {:user-id (:id user)
                                             :old-slug (:slug old)
                                             :role :administrator
                                             :created-by user-id
                                             :updated-by user-id})]
          (is (= :administrator
                 (-> (sut/get-user-with-roles database (:id user))
                     :roles
                     (get (:slug old)))))
          (testing "We can change the user into a contributor on the given OLD."
            (let [updated-user-old (sut/update-user-old
                                    database
                                    (assoc user-old :role :contributor))]
              (is (= (-> user-old
                         (assoc :role :contributor)
                         (dissoc :updated-at))
                     (dissoc updated-user-old :updated-at)))
              (is (= :contributor
                     (-> (sut/get-user-with-roles database (:id user))
                         :roles
                         (get (:slug old)))))))
          (testing "We can revoke the user's access to the OLD."
            (sut/delete-user-old database user-old)
            (is (not (-> (sut/get-user-with-roles database (:id user))
                         :roles
                         (get (:slug old))))))
          (is (= {} (:roles (sut/get-user-with-roles database (:id user)))))
          (testing "We can see the history of a user's privileges on a given OLD."
            (let [history (sut/get-user-old-history database (:id user-old))]
              (is (= [{:role :administrator :active? true}
                      {:role :contributor   :active? true}
                      {:role :contributor   :active? false}]
                     (->> history
                          (map (fn [event] {:role (-> event :row-data :role)
                                            :active? (-> event :row-data :destroyed-at nil?)}))
                          reverse)))))))
      (finally (component/stop database)))))
