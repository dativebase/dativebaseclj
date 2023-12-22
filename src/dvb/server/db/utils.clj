(ns dvb.server.db.utils)

(defn mutation
  [entity table-name]
  {:old-slug (:old-slug entity)
   :table-name table-name
   :row-id (:id entity)
   :row-data entity})
