(ns dvb.server.db.users-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            dvb.server.db.core
            [dvb.server.db.olds :as olds]
            [dvb.server.db.test-queries :as test-queries]
            [dvb.server.db.users :as sut]
            [dvb.server.system.db :as system-db]))

(defn- set-up
  "Given a clean slate, create OLD lan-old and user Alice! Then return
   database, user, and old."
  []
  (let [database (component/start
                  (system-db/make-db
                   {:name "dativebase"
                    :user "postgres"
                    :password ""}))]
    (test-queries/delete-all-the-things database)
    {:old (olds/create-old database {:slug "lan-old"
                                    :name "Language"})
     :user (sut/create-user database
                            {:first-name "Alice"
                             :last-name "Bobson"
                             :email "ab@hmail.com"
                             :username "ab"
                             :password "ENCRYPTME!!!"})
     :database database}))

(deftest users-can-be-created-read-updated-deleted
  (let [{:keys [user database database]} (set-up)]
    (try
      (testing "We can create a new user."
        (is (uuid? (:id user)))
        (is (= ["ab" "Bobson"] ((juxt :username :last-name) user)))
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
          (let [history (sut/get-history database (:id user))]
            (is (= [{:last-name "Bobson" :active? true}
                      {:last-name "Robson" :active? true}
                      {:last-name "Robson" :active? false}]
                     (->> history
                          (map (fn [event] {:last-name (-> event :row-data :last-name)
                                            :active? (-> event :row-data :destroyed-at nil?)}))
                          reverse))))))
      (finally (component/stop database)))))

(deftest users-can-be-given-roles-on-olds-and-said-roles-can-be-revoked
  (let [{:keys [old user database database]} (set-up)]
    (try
      (testing "A user's roles are an empty map when there are no roles."
        (let [user-with-roles (sut/get-user-with-roles database (:id user))]
          (is (= user (dissoc user-with-roles :roles)))
          (is (= {} (:roles user-with-roles)))))
      (testing "We can make a user into an administrator of an OLD."
        (let [user-old (sut/create-user-old database
                                            {:user-id (:id user)
                                             :old-slug (:slug old)
                                             :role :administrator})]
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

(deftest we-can-create-and-destroy-machine-users-for-users
  (let [{:keys [user database database]} (set-up)]
    (try
      (testing "A fresh user has no machine users."
        (is (empty? (sut/get-machine-users-for-user database (:id user)))))
      (testing "We can give a user a machine user, or two."
        (sut/create-machine-user database {:user-id (:id user) :api-key "key1"})
        (is (= #{"key1"}
                 (->> (sut/get-machine-users-for-user database (:id user))
                      (map :api-key) set)))
        (let [second-machine-user
              (sut/create-machine-user database {:user-id (:id user) :api-key "key2"})]
          (is (= #{"key1" "key2"}
                   (->> (sut/get-machine-users-for-user database (:id user))
                        (map :api-key) set)))
          (testing "We can destroy a machine user."
            (sut/delete-machine-user database second-machine-user)
            (is (= #{"key1"}
                     (->> (sut/get-machine-users-for-user database (:id user))
                          (map :api-key) set))))))
      (finally (component/stop database)))))
