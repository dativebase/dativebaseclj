(ns old.test-data
  (:require [com.stuartsierra.component :as component]
            [old.db.forms :as db.forms]
            [old.db.olds :as db.olds]
            [old.db.test-queries :as test-queries]
            [old.db.users :as db.users]
            [old.encrypt :as encrypt]
            [old.specs.forms :as form-specs]
            [old.specs.olds :as old-specs]
            [old.specs.users :as user-specs]
            [old.system.db :as system-db]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

(defn gen-old
  ([] (gen-old {}))
  ([overrides] (merge (gen/generate (s/gen ::old-specs/old))
                      overrides)))

(defn gen-user
  ([] (gen-user {}))
  ([overrides] (update
                (merge (gen/generate (s/gen ::user-specs/user))
                       overrides)
                :password
                encrypt/hashpw)))

(defn gen-form
  ([] (gen-form {}))
  ([overrides] (merge (gen/generate (s/gen ::form-specs/form))
                      overrides)))

(defn db-component []
  (component/start (system-db/map->DB {:db-name :old})))

(defn create-fresh-db [db-conn]
  (test-queries/delete-all-the-things db-conn)
  (let [old (db.olds/create-old
             db-conn {:slug "lan-old"
                      :name "Language"})
        user (db.users/create-user
              db-conn
              {:first-name "Alice"
               :last-name "Bobson"
               :email "ab@hmail.com"
               :username "ab"
               :password (encrypt/hashpw "password")})
        user-old (db.users/create-user-old
                  db-conn
                  {:user-id (:id user)
                   :old-slug (:slug old)
                   :role :administrator})
        machine-user (db.users/create-machine-user
                      db-conn
                      {:user-id (:id user)
                       :api-key (encrypt/hashpw "dativeold")})]
    {:old old
     :user user
     :user-old user-old
     :machine-user machine-user}))

(defn create-test-data
  "Create an OLD, a user and 10 randomly generated forms under that OLD, created
   by that tuser."
  []
  (let [db-component (db-component)
        db-conn (system-db/conn db-component)]
    (try
      (let [{:as ctx :keys [old user]} (create-fresh-db db-conn)
            {:keys [slug]} old
            {user-id :id} user]
        (assoc ctx
               :forms (mapv (fn [_]
                              (db.forms/create-form
                               db-conn
                               (gen-form {:old-slug slug
                                          :created-by-user-id user-id})))
                            (range 10))))
      (finally (component/stop db-component)))))

(comment

  (create-test-data)

)
