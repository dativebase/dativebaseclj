(ns old.db.users-test
  (:require [clojure.test :as t]
            [com.stuartsierra.component :as component]
            old.db.core
            [old.db.olds :as olds]
            [old.db.test-queries :as test-queries]
            [old.db.users :as sut]
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
     :user (sut/create-user db-conn
                            {:first-name "Alice"
                             :last-name "Bobson"
                             :email "ab@hmail.com"
                             :username "ab"
                             :password "ENCRYPTME!!!"})
     :db-conn db-conn
     :db-component db-component}))

(t/deftest users-can-be-created-read-updated-deleted
  (let [{:keys [user db-conn db-component]} (set-up)]
    (try
      (t/testing "We can create a new user."
        (t/is (uuid? (:id user)))
        (t/is (= ["ab" "Bobson"] ((juxt :username :last-name) user)))
        (t/is (inst? (:created-at user)))
        (t/is (inst? (:updated-at user)))
        (t/is (= (:updated-at user) (:created-at user)))
        (t/is (nil? (:destroyed-at user)))
        (t/testing "We can query our newly created user."
          (let [selected-user (sut/get-user db-conn (:id user))]
            (t/is (= user selected-user))))
        (t/testing "We can update the user."
          (let [updated-user (sut/update-user db-conn (assoc user :last-name "Robson"))]
            (t/is (= (-> user (assoc :last-name "Robson") (dissoc :updated-at))
                     (dissoc updated-user :updated-at)))
            (t/testing "We can (soft) delete the user."
              (let [deleted-user (sut/delete-user db-conn user)]
                (t/is (= (dissoc updated-user :updated-at :destroyed-at)
                         (dissoc deleted-user :updated-at :destroyed-at)))
                (t/is (:destroyed-at deleted-user))
                (t/is (nil? (sut/get-user db-conn (:id user))))))))
        (t/testing "We can view the history of the user."
          (let [history (sut/get-history db-conn (:id user))]
            (t/is (= [{:last-name "Bobson" :active? true}
                      {:last-name "Robson" :active? true}
                      {:last-name "Robson" :active? false}]
                     (->> history
                          (map (fn [event] {:last-name (-> event :row-data :last-name)
                                            :active? (-> event :row-data :destroyed-at nil?)}))
                          reverse))))))
      (finally (component/stop db-component)))))

(t/deftest users-can-be-given-roles-on-olds-and-said-roles-can-be-revoked
  (let [{:keys [old user db-conn db-component]} (set-up)]
    (try
      (t/testing "A user's roles are an empty map when there are no roles."
        (let [user-with-roles (sut/get-user-with-roles db-conn (:id user))]
          (t/is (= user (dissoc user-with-roles :roles)))
          (t/is (= {} (:roles user-with-roles)))))
      (t/testing "We can make a user into an administrator of an OLD."
        (let [user-old (sut/create-user-old db-conn
                                            {:user-id (:id user)
                                             :old-slug (:slug old)
                                             :role :administrator})]
          (t/is (= :administrator
                   (-> (sut/get-user-with-roles db-conn (:id user))
                       :roles
                       (get (:slug old)))))
          (t/testing "We can change the user into a contributor on the given OLD."
            (let [updated-user-old (sut/update-user-old
                                    db-conn
                                    (assoc user-old :role :contributor))]
              (t/is (= (-> user-old
                           (assoc :role :contributor)
                           (dissoc :updated-at))
                       (dissoc updated-user-old :updated-at)))
              (t/is (= :contributor
                       (-> (sut/get-user-with-roles db-conn (:id user))
                           :roles
                           (get (:slug old)))))))
          (t/testing "We can revoke the user's access to the OLD."
            (sut/delete-user-old db-conn user-old)
            (t/is (not (-> (sut/get-user-with-roles db-conn (:id user))
                           :roles
                           (get (:slug old))))))
          (t/is (= {} (:roles (sut/get-user-with-roles db-conn (:id user)))))
          (t/testing "We can see the history of a user's privileges on a given OLD."
            (let [history (sut/get-user-old-history db-conn (:id user-old))]
              (t/is (= [{:role :administrator :active? true}
                        {:role :contributor   :active? true}
                        {:role :contributor   :active? false}]
                       (->> history
                            (map (fn [event] {:role (-> event :row-data :role)
                                              :active? (-> event :row-data :destroyed-at nil?)}))
                            reverse)))))))
      (finally (component/stop db-component)))))

(t/deftest we-can-create-and-destroy-machine-users-for-users
  (let [{:keys [user db-conn db-component]} (set-up)]
    (try
      (t/testing "A fresh user has no machine users."
        (t/is (empty? (sut/get-machine-users-for-user db-conn (:id user)))))
      (t/testing "We can give a user a machine user, or two."
        (sut/create-machine-user db-conn {:user-id (:id user) :api-key "key1"})
        (t/is (= #{"key1"}
                 (->> (sut/get-machine-users-for-user db-conn (:id user))
                      (map :api-key) set)))
        (let [second-machine-user
              (sut/create-machine-user db-conn {:user-id (:id user) :api-key "key2"})]
          (t/is (= #{"key1" "key2"}
                   (->> (sut/get-machine-users-for-user db-conn (:id user))
                        (map :api-key) set)))
          (t/testing "We can destroy a machine user."
            (sut/delete-machine-user db-conn second-machine-user)
            (t/is (= #{"key1"}
                     (->> (sut/get-machine-users-for-user db-conn (:id user))
                          (map :api-key) set))))))
      (finally (component/stop db-component)))))
