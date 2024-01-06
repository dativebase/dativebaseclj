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
         delete-user*
         get-user*
         get-users*
         get-user-by-email*
         get-user-with-roles*
         get-user-with-plans*
         update-user*)

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

(defn count-users [db-conn]
  (:user-count (count-users* db-conn)))

(defn get-users
  ([db-conn] (get-users db-conn 10))
  ([db-conn limit] (get-users db-conn limit 0))
  ([db-conn limit offset]
   (mapv edges/user-pg->clj
         (get-users* db-conn {:limit limit
                              :offset offset}))))
