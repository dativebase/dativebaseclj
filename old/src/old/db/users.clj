(ns old.db.users
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [hugsql.core :as hugsql]
            [old.db.events :as events]
            [old.db.utils :as utils]))

(declare get-machine-user*
         get-machine-users-for-user*
         get-user*
         get-user-with-roles*
         create-machine-user*
         create-user*
         create-user-old*
         delete-machine-user*
         delete-user*
         delete-user-old*
         update-user*
         update-user-old*)

(hugsql/def-db-fns "sql/users.sql")

(defn get-user [db-conn id] (get-user* db-conn {:id id}))

(defn- user-row->user-entity [user-row]
  (-> user-row
      utils/db-row->entity
      (set/rename-keys {:first_name :first-name
                        :last_name :last-name})))

(defn get-user-with-roles [db-conn id]
  (let [[user :as rows] (get-user-with-roles* db-conn {:id id})]
    (when user
      (-> user
          (dissoc :role :old-slug)
          (assoc :roles (->> rows
                             (filter :old-slug)
                             (map (juxt :old-slug (comp keyword :role)))
                             (into {})))))))

(defn- mutate [mutation db-conn user]
  (jdbc/with-db-transaction [tconn db-conn]
    (let [[db-user] ((case mutation
                       :create create-user*
                       :update update-user*
                       :delete delete-user*) tconn user)
          user (user-row->user-entity db-user)]
      (events/create-event tconn (utils/mutation user "users"))
      user)))

(def create-user (partial mutate :create))

(def update-user (partial mutate :update))

(def delete-user (partial mutate :delete))

(defn get-history [db-conn id]
  (events/get-history db-conn nil "users" id))

(defn- user-old-row->user-old-entity [user-old-row]
  (-> user-old-row
      utils/db-row->entity
      (set/rename-keys {:user_id :user-id
                        :old_slug :old-slug})
      (update :role keyword)))

(defn- user-old-entity->user-old-row [user-old-entity]
  (update user-old-entity :role name))

(defn- mutate-user-old [mutation db-conn user-old]
  (jdbc/with-db-transaction [tconn db-conn]
    (let [[db-user-old] ((case mutation
                           :create create-user-old*
                           :update update-user-old*
                           :delete delete-user-old*)
                         tconn
                         (user-old-entity->user-old-row user-old))
          user-old (user-old-row->user-old-entity db-user-old)]
      (events/create-event tconn (assoc (utils/mutation user-old "users_olds") :old-slug nil))
      user-old)))

(def create-user-old (partial mutate-user-old :create))

(def update-user-old (partial mutate-user-old :update))

(def delete-user-old (partial mutate-user-old :delete))

(defn get-user-old-history [db-conn id]
  (events/get-history db-conn nil "users_olds" id))

(defn- machine-user-row->machine-user-entity [machine-user-row]
  (-> machine-user-row
      utils/db-row->entity
      (set/rename-keys {:user_id :user-id
                        :api_key :api-key})))

(defn- mutate-machine-user [mutation db-conn machine-user]
  (jdbc/with-db-transaction [tconn db-conn]
    (let [[db-machine-user] ((case mutation
                               :create create-machine-user*
                               :delete delete-machine-user*)
                             tconn
                             machine-user)
          machine-user (machine-user-row->machine-user-entity db-machine-user)]
      (events/create-event tconn (utils/mutation machine-user "machine_users"))
      machine-user)))

(def create-machine-user (partial mutate-machine-user :create))

(def delete-machine-user (partial mutate-machine-user :delete))

(defn get-machine-users-for-user [db-conn user-id]
  (get-machine-users-for-user* db-conn {:user-id user-id}))

(defn get-machine-user [db-conn id]
  (get-machine-user* db-conn {:id id}))
