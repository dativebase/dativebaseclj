(ns dvb.server.db.olds
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.server.db.events :as events]
            [hugsql.core :as hugsql]))

(declare count-olds*
         get-old*
         get-olds*
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

(defn count-olds [db-conn]
  (:old-count (count-olds* db-conn)))

(defn get-olds
  ([db-conn] (get-olds db-conn 10))
  ([db-conn limit] (get-olds db-conn limit 0))
  ([db-conn limit offset]
   (get-olds* db-conn {:limit limit
                       :offset offset})))

(defn old-admins [old-with-users]
  (when-not (contains? old-with-users :users)
    (throw (ex-info "Fn old-admins only works on an OLD with a :users key"
                    {:old-with-users old-with-users})))
  (->> old-with-users
       :users
       (filterv (comp (partial = :administrator) :role))))

(defn old-admin-ids [old-with-users]
  (mapv :id (old-admins old-with-users)))
