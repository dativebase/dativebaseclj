(ns dvb.server.db.plans
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.edges.plans :as plan-edges]
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
         most-recent-plan-created-by-ip-address*
         delete-plan*)

(hugsql/def-db-fns "sql/plans.sql")

(defn get-plan [db-conn id]
  (plan-edges/pg->clj (get-plan* db-conn {:id id})))

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
          plan-edges/pg->clj)
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
          plan-edges/pg->clj)
      (when-let [plan (get-plan db-conn id)]
        (assoc plan :olds [])))))

(defn get-plan-with-olds-and-members [db-conn id]
  (assoc (get-plan-with-olds db-conn id)
         :members (:members (get-plan-with-members db-conn id))))

(defn most-recent-plan-created-by-ip-address [db-conn ip-address]
  (plan-edges/pg->clj (most-recent-plan-created-by-ip-address*
                       db-conn
                       {:created-by-ip-address ip-address})))

(defn- mutate [mutation db-conn plan]
  (jdbc/with-db-transaction [tconn db-conn]
    (let [db-plan ((case mutation
                     :create create-plan*
                     :update update-plan*
                     :delete delete-plan*)
                   tconn
                   (plan-edges/clj->pg plan))
          plan (plan-edges/pg->clj db-plan)]
      (events/create-event tconn (utils/mutation plan "plans"))
      plan)))

(defn create-plan [db-conn plan]
  (mutate :create
          db-conn
          (update plan
                  :created-by-ip-address
                  (fnil identity "unknown"))))

(def update-plan (partial mutate :update))

(def delete-plan (partial mutate :delete))

(defn get-history [db-conn id]
  (events/get-history db-conn id "plans" nil))

(defn get-plans
  ([db-conn] (get-plans db-conn 10))
  ([db-conn limit] (get-plans db-conn limit 0))
  ([db-conn limit offset]
   (mapv plan-edges/pg->clj
         (get-plans* db-conn {:limit limit
                              :offset offset}))))

(defn get-plans-for-user [db-conn user-id]
  (get-plans-for-user* db-conn {:user-id user-id}))

(defn plan-managers [plan-with-members]
  (when-not (contains? plan-with-members :members)
    (throw (ex-info "Fn plan-managers only works on a plan with a :members key"
                    {:plan-with-members plan-with-members})))
  (->> plan-with-members
       :members
       (filterv (comp (partial = :manager) :role))))

(defn plan-manager-ids [plan-with-members]
  (mapv :id (plan-managers plan-with-members)))
