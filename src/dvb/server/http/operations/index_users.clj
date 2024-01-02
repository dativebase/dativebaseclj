(ns dvb.server.http.operations.index-users
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.edges :as edges]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as u]
            [dvb.server.http.utils.pagination :as pagination]
            [dvb.server.log :as log]))

(defn handle [{:as _application :keys [database]} ctx]
  (log/info "Indexing users.")
  (authorize/authorize ctx)
  (let [{:keys [is-superuser?]} (u/security-user ctx)
        {{:keys [page items-per-page]} :query} ctx]
    (jdbc/with-db-transaction [tconn database {:isolation :repeatable-read}]
      (let [user-count (db.users/count-users tconn)
            users (cond->> (db.users/get-users
                            tconn
                            items-per-page
                            (pagination/offset! page items-per-page user-count))
                    (not is-superuser?) (map u/minimize-user)
                    :always (mapv edges/user-clj->api))]
        {:status 200
         :headers {}
         :body {:data users
                :meta {:count user-count
                       :page page
                       :items-per-page items-per-page}}}))))
