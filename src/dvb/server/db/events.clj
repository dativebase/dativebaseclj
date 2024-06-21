(ns dvb.server.db.events
  (:require [dvb.server.utils :as utils]
            [hugsql.core :as hugsql]))

(declare get-history*
         get-history-of-global*
         get-history-of-old*
         insert-event*)

(hugsql/def-db-fns "sql/events.sql")

(defn create-event [db-conn event]
  (insert-event* db-conn (update event :row-data pr-str)))

(defn get-history
  "Return the history of the referenced entity. Return at most `limit` events,
  ordered by `created-at` timestamp from most recent to oldest. The serialized
  data under `:row-data` is deserialized to a Clojure data structure."
  ([db-conn old-slug table-name row-id]
   (get-history db-conn old-slug table-name row-id 10))
  ([db-conn old-slug table-name row-id limit]
   (let [getter-fn (cond (and old-slug row-id) get-history*
                         old-slug get-history-of-old*
                         :else get-history-of-global*)
         events (getter-fn db-conn
                           {:old-slug (some-> old-slug name)
                            :table-name table-name
                            :row-id row-id
                            :limit limit})]
     (mapv (fn [event] (update event :row-data utils/read-string))
           events))))
