(ns dvb.server.db.users
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.edges :as edges]
            [dvb.server.db.events :as events]
            [dvb.server.db.utils :as utils]
            [dvb.server.encrypt :as encrypt]
            [hugsql.core :as hugsql]))

(declare activate-user*
         count-users*
         create-user*
         create-user-old*
         delete-user*
         delete-user-old*
         get-user*
         get-users*
         get-user-by-email*
         get-user-with-roles*
         get-user-with-plans*
         update-user*
         update-user-old*)

(hugsql/def-db-fns "sql/users.sql")

(defn get-user [db-conn id]
  (edges/user-pg->clj (get-user* db-conn {:id id})))

(defn get-user-by-email [db-conn email]
  (edges/user-pg->clj (get-user-by-email* db-conn {:email email})))

(defn get-user-with-roles [db-conn id]
  (let [[user :as rows] (get-user-with-roles* db-conn {:id id})]
    (when user
      (-> user
          edges/user-pg->clj
          (dissoc :role :old-slug)
          (assoc :roles (->> rows
                             (filter :old-slug)
                             (map (juxt :old-slug (comp keyword :role)))
                             (into {})))))))

(defn get-user-with-plans [db-conn id]
  (let [[user :as rows] (get-user-with-plans* db-conn {:id id})]
    (if user
      (-> user
          (dissoc :role :tier :plan-id :user-plan-id)
          (assoc :plans (->> rows
                             (mapv (fn [{:keys [role tier plan-id user-plan-id]}]
                                     {:role role
                                      :tier tier
                                      :id plan-id
                                      :user-plan-id user-plan-id}))))
          edges/user-pg->clj)
      (when-let [user (get-user db-conn id)]
        (assoc user :plans [])))))

(defn- hash-user-password [user]
  (if (:password user)
    (update user :password encrypt/hashpw)
    user))

(defn- mutate [mutation db-conn user]
  (let [user (hash-user-password user)]
    (jdbc/with-db-transaction [tconn db-conn]
      (let [db-user ((case mutation
                       :create create-user*
                       :update update-user*
                       :delete delete-user*
                       :activate activate-user*) tconn user)
            user (edges/user-pg->clj db-user)]
        (events/create-event tconn (utils/mutation user "users"))
        user))))

(def create-user (partial mutate :create))

(def update-user (partial mutate :update))

(def delete-user (partial mutate :delete))

(def activate-user (partial mutate :activate))

(defn get-history [db-conn id]
  (events/get-history db-conn nil "users" id))

(defn- user-old-row->user-old-entity [user-old-row]
  (update user-old-row :role keyword))

(defn- user-old-entity->user-old-row [user-old-entity]
  (update user-old-entity :role name))

(defn- mutate-user-old [mutation db-conn user-old]
  (jdbc/with-db-transaction [tconn db-conn]
    (let [db-user-old ((case mutation
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

(defn count-users [db-conn]
  (:user-count (count-users* db-conn)))

(defn get-users
  ([db-conn] (get-users db-conn 10))
  ([db-conn limit] (get-users db-conn limit 0))
  ([db-conn limit offset]
   (mapv edges/user-pg->clj
         (get-users* db-conn {:limit limit
                              :offset offset}))))
