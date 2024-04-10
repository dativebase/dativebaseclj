(ns dvb.server.db.utils)

(defn mutation
  [entity table-name]
  {:old-slug (when-let [slug (:old-slug entity)] (name slug))
   :table-name table-name
   :row-id (:id entity)
   :row-data entity})
