(ns dvb.server.db.olds
  (:require [clojure.java.jdbc :as jdbc]
            [hugsql.core :as hugsql]
            [dvb.server.db.events :as events]))

(declare get-old*
         get-old-with-users*
         delete-old*
         create-old*
         update-old*)

(hugsql/def-db-fns "sql/olds.sql")

(defn get-old [db-conn slug] (get-old* db-conn {:slug slug}))

(defn get-old-with-users [db-conn slug]
  (let [[old :as rows] (get-old-with-users* db-conn {:slug slug})]
    (if old
      (-> old
          (dissoc :role :user-id :user-old-id)
          (assoc :users (mapv (fn [{:keys [role user-id user-old-id]}]
                                {:role (keyword role)
                                 :id user-id
                                 :user-old-id user-old-id})
                              rows)))
      (when-let [old (get-old db-conn slug)]
        (assoc old :users [])))))

(defn- mutate [mutation db-conn old]
  (jdbc/with-db-transaction [tconn db-conn]
    (let [old ((case mutation
                 :create create-old*
                 :update update-old*
                 :delete delete-old*) tconn old)]
      (events/create-event tconn
                           {:old-slug (:slug old)
                            :table-name "olds"
                            :row-id nil
                            :row-data old})
      old)))

(def create-old (partial mutate :create))

(def update-old (partial mutate :update))

(def delete-old (partial mutate :delete))

(defn get-history [db-conn slug]
  (events/get-history db-conn slug "olds" nil))
