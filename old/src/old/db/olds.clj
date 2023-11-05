(ns old.db.olds
  (:require [clojure.java.jdbc :as jdbc]
            [hugsql.core :as hugsql]
            [old.db.events :as events]
            [old.db.utils :as utils]))

(declare get-old*
         delete-old*
         create-old*
         update-old*)

(hugsql/def-db-fns "sql/olds.sql")

(defn get-old [db-conn slug] (get-old* db-conn {:slug slug}))

(defn- mutate [mutation db-conn old]
  (jdbc/with-db-transaction [tconn db-conn]
    (let [[db-old] ((case mutation
                       :create create-old*
                       :update update-old*
                       :delete delete-old*) tconn old)
          old (utils/db-row->entity db-old)]
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
