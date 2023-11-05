(ns old.db.events
  (:require [hugsql.core :as hugsql]))

(declare get-history*
         get-history-of-global*
         get-history-of-old*
         insert-event*)

(hugsql/def-db-fns "sql/events.sql")

(defn create-event [db-conn event]
  (insert-event* db-conn (update event :row-data pr-str)))

(defn get-history
  ([db-conn old-slug table-name row-id] (get-history db-conn old-slug table-name row-id 10))
  ([db-conn old-slug table-name row-id limit]
   (let [events ((cond (and old-slug row-id) get-history*
                       old-slug get-history-of-old*
                       :else get-history-of-global*)
                 db-conn {:old-slug old-slug
                          :table-name table-name
                          :row-id row-id
                          :limit limit})]
     (mapv (fn [event] (update event :row-data read-string)) events))))
