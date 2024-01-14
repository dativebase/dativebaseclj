(ns dvb.server.db.old-access-requests
  (:require [dvb.common.edges.old-access-requests :as old-access-request-edges]
            [hugsql.core :as hugsql]))

(declare approve*
         count-old-access-requests*
         create-old-access-request*
         get-old-access-request*
         get-pending-old-access-requests-for-user*
         get-pending-old-access-requests-for-old*
         get-old-access-requests*
         reject*
         retract*)

(hugsql/def-db-fns "sql/old_access_requests.sql")

(defn get-old-access-request [db-conn id]
  (old-access-request-edges/pg->clj
   (get-old-access-request* db-conn {:id id})))

(defn create-old-access-request [db-conn old-access-request]
  (old-access-request-edges/pg->clj
   (create-old-access-request*
    db-conn
    (old-access-request-edges/clj->pg old-access-request))))

(defn approve [db-conn id]
  (old-access-request-edges/pg->clj (approve* db-conn {:id id})))

(defn reject [db-conn id]
  (old-access-request-edges/pg->clj (reject* db-conn {:id id})))

(defn retract [db-conn id]
  (old-access-request-edges/pg->clj (retract* db-conn {:id id})))

(defn count-old-access-requests [db-conn]
  (:old-access-request-count (count-old-access-requests* db-conn)))

(defn get-old-access-requests
  ([db-conn] (get-old-access-requests db-conn 10))
  ([db-conn limit] (get-old-access-requests db-conn limit 0))
  ([db-conn limit offset]
   (mapv old-access-request-edges/pg->clj
         (get-old-access-requests* db-conn {:limit limit
                                            :offset offset}))))

(defn get-pending-old-access-requests-for-user [db-conn user-id]
  (mapv old-access-request-edges/pg->clj
        (get-pending-old-access-requests-for-user* db-conn {:user-id user-id})))

(defn get-pending-old-access-requests-for-old [db-conn old-slug]
  (mapv old-access-request-edges/pg->clj
        (get-pending-old-access-requests-for-old* db-conn {:old-slug old-slug})))
