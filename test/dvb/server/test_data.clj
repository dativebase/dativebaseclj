(ns dvb.server.test-data
  (:require [com.stuartsierra.component :as component]
            [dvb.server.db.forms :as db.forms]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.test-queries :as test-queries]
            [dvb.server.db.users :as db.users]
            [dvb.server.encrypt :as encrypt]
            [dvb.server.specs.forms :as form-specs]
            [dvb.server.specs.olds :as old-specs]
            [dvb.server.specs.users :as user-specs]
            [dvb.server.system.db :as system-db]
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
  (component/start
   (system-db/make-db
    {:name "dativebase"
     :user "postgres"
     :password ""})))

(defn create-fresh-db [database]
  (test-queries/delete-all-the-things database)
  (let [old (db.olds/create-old
             database {:slug "lan-old"
                      :name "Language"})
        user (db.users/create-user
              database
              {:first-name "Alice"
               :last-name "Bobson"
               :email "ab@hmail.com"
               :username "ab"
               :password (encrypt/hashpw "password")})
        user-old (db.users/create-user-old
                  database
                  {:user-id (:id user)
                   :old-slug (:slug old)
                   :role :administrator})
        machine-user (db.users/create-machine-user
                      database
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
  (let [database (db-component)]
    (try
      (let [{:as ctx :keys [old user]} (create-fresh-db database)
            {:keys [slug]} old
            {user-id :id} user]
        (assoc ctx
               :forms (mapv (fn [_]
                              (db.forms/create-form
                               database
                               (gen-form {:old-slug slug
                                          :created-by user-id})))
                            (range 10))))
      (finally (component/stop db-component)))))

(comment

  (create-test-data)

)
