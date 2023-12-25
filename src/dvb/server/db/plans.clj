(ns dvb.server.db.plans
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.edges :as edges]
            [dvb.server.db.events :as events]
            [dvb.server.db.utils :as utils]
            [hugsql.core :as hugsql]))

(declare create-plan*
         update-plan*
         get-plan*
         get-plans*
         delete-plan*)

(hugsql/def-db-fns "sql/plans.sql")

(defn get-plan [db-conn id]
  (edges/plan-pg->clj (get-plan* db-conn {:id id})))

(defn- mutate [mutation db-conn plan]
  (jdbc/with-db-transaction [tconn db-conn]
    (let [db-plan ((case mutation
                     :create create-plan*
                     :update update-plan*
                     :delete delete-plan*)
                   tconn
                   (edges/plan-clj->pg plan))
          plan (edges/plan-pg->clj db-plan)]
      (events/create-event tconn (utils/mutation plan "plans"))
      plan)))

(def create-plan (partial mutate :create))

(def update-plan (partial mutate :update))

(def delete-plan (partial mutate :delete))

(defn get-history [db-conn id]
  (events/get-history db-conn id "plans" nil))

(defn get-plans
  ([db-conn] (get-plans db-conn 10))
  ([db-conn limit] (get-plans db-conn limit 0))
  ([db-conn limit offset]
   (mapv edges/plan-pg->clj
         (get-plans* db-conn {:limit limit
                              :offset offset}))))
