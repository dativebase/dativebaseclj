(ns dvb.server.db.user-olds
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.edges.user-olds :as user-old-edges]
            [dvb.server.db.events :as events]
            [dvb.server.db.utils :as utils]
            [hugsql.core :as hugsql]))

(declare create-user-old*
         delete-user-old*
         get-user-old*
         get-user-olds*
         update-user-old*)

(hugsql/def-db-fns "sql/user_olds.sql")

(defn get-user-old [db-conn id]
  (user-old-edges/pg->clj (get-user-old* db-conn {:id id})))

(defn- mutate-user-old [mutation db-conn user-old]
  (jdbc/with-db-transaction [tconn db-conn]
    (let [db-user-old ((case mutation
                         :create create-user-old*
                         :update update-user-old*
                         :delete delete-user-old*)
                       tconn
                       (user-old-edges/clj->pg user-old))
          user-old (user-old-edges/pg->clj db-user-old)]
      (events/create-event
       tconn
       (assoc (utils/mutation user-old "users_olds") :old-slug nil))
      user-old)))

(def create-user-old (partial mutate-user-old :create))

(def update-user-old (partial mutate-user-old :update))

(def delete-user-old (partial mutate-user-old :delete))

(defn get-user-old-history [db-conn id]
  (events/get-history db-conn nil "users_olds" id))

(defn get-user-olds
  ([db-conn] (get-user-olds db-conn 10))
  ([db-conn limit] (get-user-olds db-conn limit 0))
  ([db-conn limit offset]
   (mapv user-old-edges/pg->clj
         (get-user-olds* db-conn {:limit limit
                                   :offset offset}))))
