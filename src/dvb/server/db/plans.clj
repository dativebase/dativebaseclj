(ns dvb.server.db.plans
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.edges :as edges]
            [dvb.server.db.events :as events]
            [dvb.server.db.utils :as utils]
            [hugsql.core :as hugsql]))

(declare create-plan*
         update-plan*
         get-plan*
         get-plan-with-members*
         get-plan-with-olds*
         get-plans*
         get-plans-for-user*
         delete-plan*)

(hugsql/def-db-fns "sql/plans.sql")

(defn get-plan [db-conn id]
  (edges/plan-pg->clj (get-plan* db-conn {:id id})))

(defn get-plan-with-members [db-conn id]
  (let [[plan :as rows] (get-plan-with-members* db-conn {:id id})]
    (if plan
      (-> plan
          (dissoc :role :user-id :user-plan-id)
          (assoc :members (mapv (fn [{:keys [role user-id user-plan-id]}]
                                  {:role role
                                   :id user-id
                                   :user-plan-id user-plan-id})
                                rows))
          edges/plan-pg->clj)
      (when-let [plan (get-plan db-conn id)]
        (assoc plan :members [])))))

(defn get-plan-with-olds [db-conn id]
  (let [[plan :as rows] (get-plan-with-olds* db-conn {:id id})]
    (if plan
      (-> plan
          (dissoc :old-slug)
          (assoc :olds (->> rows
                            (map :old-slug)
                            (filter some?)
                            set sort vec))
          edges/plan-pg->clj)
      (when-let [plan (get-plan db-conn id)]
        (assoc plan :olds [])))))

(defn get-plan-with-olds-and-members [db-conn id]
  (assoc (get-plan-with-olds db-conn id)
         :members (:members (get-plan-with-members db-conn id))))

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

(defn get-plans-for-user [db-conn user-id]
  (get-plans-for-user* db-conn {:user-id user-id}))

;; TODO: should probably rename to plan-manager-ids
(defn plan-managers [plan]
  (->> plan
       :members
       (filter (comp (partial = :manager) :role))
       (map :id)))
