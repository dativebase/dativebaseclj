(ns old.http.operations.index-forms
  (:require [clojure.java.jdbc :as jdbc]
            [old.system.db :as system-db]
            [old.db.forms :as db.forms]
            [old.http.authorize :as authorize]
            [old.http.operations.utils :as utils]
            [old.http.utils.pagination :as pagination]
            [taoensso.timbre :as log]))

(defn handle [{:as _application :keys [database]} ctx]
  (log/info "Indexing forms.")
  (authorize/authorize ctx)
  (let [db-conn (system-db/conn database)
        old-slug (-> ctx :path :old_slug)
        {{:keys [page items-per-page]} :query} ctx]
    (jdbc/with-db-transaction [tconn db-conn {:isolation :repeatable-read}]
      (let [form-count (db.forms/count-forms tconn old-slug)
            forms (db.forms/get-forms tconn old-slug items-per-page
                                      (pagination/offset! page items-per-page form-count))]
        {:status 200
         :headers {}
         :body {:data (mapv utils/declojurify-form forms)
                :meta {:count form-count
                       :page page
                       :items-per-page items-per-page}}}))))
