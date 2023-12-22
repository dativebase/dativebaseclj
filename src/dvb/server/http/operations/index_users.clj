(ns dvb.server.http.operations.index-users
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils.declojurify :as declojurify]
            [dvb.server.http.utils.pagination :as pagination]
            [dvb.server.log :as log]))

(defn handle [{:as _application :keys [database]} ctx]
  (log/info "Indexing users.")
  (authorize/authorize ctx)
  (let [{{:keys [page items-per-page]} :query} ctx]
    (jdbc/with-db-transaction [tconn database {:isolation :repeatable-read}]
      (let [user-count (db.users/count-users tconn)
            users (db.users/get-users
                   tconn
                   items-per-page
                   (pagination/offset! page items-per-page user-count))]
        {:status 200
         :headers {}
         :body {:data (mapv declojurify/user users)
                :meta {:count user-count
                       :page page
                       :items-per-page items-per-page}}}))))
