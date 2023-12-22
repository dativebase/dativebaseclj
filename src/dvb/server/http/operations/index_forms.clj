(ns dvb.server.http.operations.index-forms
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.server.db.forms :as db.forms]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils.declojurify :as declojurify]
            [dvb.server.http.utils.pagination :as pagination]
            [dvb.server.log :as log]))

(defn handle [{:as _application :keys [database]} ctx]
  (log/info "Indexing forms.")
  (authorize/authorize ctx)
  (let [old-slug (-> ctx :path :old_slug)
        {{:keys [page items-per-page]} :query} ctx]
    (jdbc/with-db-transaction [tconn database {:isolation :repeatable-read}]
      (let [form-count (db.forms/count-forms tconn old-slug)
            forms (db.forms/get-forms tconn old-slug items-per-page
                                      (pagination/offset! page items-per-page form-count))]
        {:status 200
         :headers {}
         :body {:data (mapv declojurify/form forms)
                :meta {:count form-count
                       :page page
                       :items-per-page items-per-page}}}))))
