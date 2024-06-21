(ns dvb.server.db.user-olds-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.users :as db.users]
            [dvb.server.db.user-olds :as sut]
            [dvb.server.test-data :as test-data]))

(deftest user-roles-work
  (let [{:keys [old user database]} (test-data/set-up-old-user)
        {user-id :id} user]
    (try
      (testing "A user's roles are an empty map when there are no roles."
        (let [user-with-roles (db.users/get-user-with-roles database (:id user))]
          (is (= user (dissoc user-with-roles :roles)))
          (is (= {} (:roles user-with-roles)))))
      (testing "An OLD's users are an empty map when there are no roles."
        (let [old-with-users (db.olds/get-old-with-users database (:slug old))]
          (is (= old (dissoc old-with-users :users)))
          (is (= [] (:users old-with-users)))))
      (testing "We can make a user into an administrator of an OLD."
        (let [user-old (sut/create-user-old database
                                            {:user-id (:id user)
                                             :old-slug (:slug old)
                                             :role :administrator
                                             :created-by user-id
                                             :updated-by user-id})]
          (is (= :administrator
                 (-> (db.users/get-user-with-roles database (:id user))
                     :roles
                     (get (:slug old)))))
          (is (= :administrator
                 (->> (db.olds/get-old-with-users database (:slug old))
                      :users
                      (filter (comp (partial = (:id user)) :id))
                      first
                      :role)))
          (testing "We can change the user into a contributor on the given OLD."
            (let [updated-user-old (sut/update-user-old
                                    database
                                    (assoc user-old :role :contributor))]
              (is (= (-> user-old
                         (assoc :role :contributor)
                         (dissoc :updated-at))
                     (dissoc updated-user-old :updated-at)))
              (is (= :contributor
                     (-> (db.users/get-user-with-roles database (:id user))
                         :roles
                         (get (:slug old)))))
              (is (= :contributor
                     (->> (db.olds/get-old-with-users database (:slug old))
                          :users
                          (filter (comp (partial = (:id user)) :id))
                          first
                          :role)))))
          (testing "We can revoke the user's access to the OLD."
            (sut/delete-user-old database user-old)
            (is (not (-> (db.users/get-user-with-roles database (:id user))
                         :roles
                         (get (:slug old)))))
            (is (empty? (:users (db.olds/get-old-with-users database (:slug old))))))
          (is (= {} (:roles (db.users/get-user-with-roles database (:id user)))))
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
