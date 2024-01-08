(ns dvb.server.http.operations.index-olds
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.edges :as edges]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as u]
            [dvb.server.http.utils.pagination :as pagination]
            [dvb.server.log :as log]))

(defn handle [{:as _application :keys [database]} ctx]
  (let [authenticated-user-id (u/security-user-id ctx)]
    (log/info "Indexing OLDs." {:authenticated-user-id authenticated-user-id})
    (authorize/authorize ctx)
    (let [{{:keys [page items-per-page]} :query} ctx]
      (jdbc/with-db-transaction [tx database {:isolation :repeatable-read}]
        (let [old-count (db.olds/count-olds tx)
              olds (mapv edges/old-clj->api
                         (db.olds/get-olds
                          tx items-per-page
                          (pagination/offset! page items-per-page old-count)))]
          (log/info "Indexed OLDs." {:authenticated-user-id authenticated-user-id})
          {:status 200
           :body {:data olds
                  :meta {:count old-count
                         :page page
                         :items-per-page items-per-page}}})))))
