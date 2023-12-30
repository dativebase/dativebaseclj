(ns dvb.server.db.users-plans
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.edges :as edges]
            [dvb.server.db.events :as events]
            [dvb.server.db.utils :as utils]
            [hugsql.core :as hugsql]))

(declare create-user-plan*
         update-user-plan*
         get-user-plan*
         get-user-plans*
         delete-user-plan*)

(hugsql/def-db-fns "sql/users_plans.sql")

(defn get-user-plan [db-conn id]
  (edges/user-plan-pg->clj (get-user-plan* db-conn {:id id})))

(defn- mutate [mutation db-conn user-plan]
  (jdbc/with-db-transaction [tconn db-conn]
    (let [db-user-plan ((case mutation
                     :create create-user-plan*
                     :update update-user-plan*
                     :delete delete-user-plan*)
                   tconn
                   (edges/user-plan-clj->pg user-plan))
          user-plan (edges/user-plan-pg->clj db-user-plan)]
      (events/create-event tconn (utils/mutation user-plan "users_plans"))
      user-plan)))

(def create-user-plan (partial mutate :create))

(def update-user-plan (partial mutate :update))

(def delete-user-plan (partial mutate :delete))

(defn get-history [db-conn id]
  (events/get-history db-conn nil "users_plans" id))

(defn get-user-plans
  ([db-conn] (get-user-plans db-conn 10))
  ([db-conn limit] (get-user-plans db-conn limit 0))
  ([db-conn limit offset]
   (mapv edges/user-plan-pg->clj
         (get-user-plans* db-conn {:limit limit
                              :offset offset}))))
