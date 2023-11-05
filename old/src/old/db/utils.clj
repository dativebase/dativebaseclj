(ns old.db.utils
  (:require [clojure.set :as set]))

(defn db-row->entity [db-row]
  (set/rename-keys db-row {:created_at :created-at
                           :inserted_at :inserted-at
                           :updated_at :updated-at
                           :destroyed_at :destroyed-at}))

(defn mutation
  [entity table-name]
  {:old-slug (:old-slug entity)
   :table-name table-name
   :row-id (:id entity)
   :row-data entity})
