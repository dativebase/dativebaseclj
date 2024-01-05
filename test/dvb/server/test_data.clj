(ns dvb.server.test-data
  (:require [com.stuartsierra.component :as component]
            [dvb.common.specs.forms :as forms-specs]
            [dvb.common.specs.olds :as olds-specs]
            [dvb.common.specs.users :as users-specs]
            [dvb.server.db.forms :as db.forms]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.test-queries :as test-queries]
            [dvb.server.db.users :as db.users]
            [dvb.server.encrypt :as encrypt]
            [dvb.server.system.db :as system-db]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

(defn gen-old
  ([] (gen-old {}))
  ([overrides] (merge (gen/generate (s/gen ::olds-specs/old))
                      overrides)))

(defn gen-old-write
  ([] (gen-old-write {}))
  ([overrides] (merge (gen/generate (s/gen ::olds-specs/old-write))
                      overrides)))

(defn gen-user
  ([] (gen-user {}))
  ([overrides] (gen-user overrides {}))
  ([overrides {:keys [ensure-password?]
               :or {ensure-password? true}}]
   (merge
    (when ensure-password?
      {:password (gen/generate (s/gen ::users-specs/password))})
    (gen/generate (s/gen ::users-specs/user))
    overrides)))

(defn gen-user-write
  ([] (gen-user-write {}))
  ([overrides]
   (merge (gen/generate (s/gen ::users-specs/user-write))
          overrides)))

(defn gen-form
  ([] (gen-form {}))
  ([overrides]
   (merge (gen/generate (s/gen ::forms-specs/form))
          overrides)))

(defn gen-form-write
  ([] (gen-form-write {}))
  ([overrides]
   (merge (gen/generate (s/gen ::forms-specs/form-write))
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
                       :name "Language"
                       :plan-id nil})
        user (db.users/create-user
              database
              {:first-name "Alice"
               :last-name "Bobson"
               :email "ab@hmail.com"
               :username "ab"
               :password (encrypt/hashpw "password")
               :is-superuser? false})
        user-old (db.users/create-user-old
                  database
                  {:user-id (:id user)
                   :old-slug (:slug old)
                   :role :administrator})]
    {:old old
     :user user
     :user-old user-old}))

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
                               (gen-form-write {:old-slug slug
                                                :created-by user-id})))
                            (range 10))))
      (finally (component/stop db-component)))))

(defn set-up-old-user []
  (let [database (db-component)
        {:as user user-id :id}
        (db.users/create-user database (gen-user-write
                                        {:created-by nil
                                         :updated-by nil}))]
    {:user user
     :old (db.olds/create-old database (gen-old-write
                                        {:created-by user-id
                                         :updated-by user-id
                                         :plan-id nil}))
     :database database}))

(comment

  (create-test-data)

)
