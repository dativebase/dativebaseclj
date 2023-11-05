(ns old.http.operations.create-form
  (:require [old.system.db :as system-db]
            [old.db.forms :as db.forms]
            [old.http.authorize :as authorize]
            [old.http.openapi.errors :as errors]
            [old.http.operations.utils :as utils]
            [taoensso.timbre :as log]))

(defn handle [{:keys [database]} ctx]
  (log/info "Create a form.")
  (authorize/authorize ctx)
  (let [db-conn (system-db/conn database)
        old-slug (-> ctx :path :old_slug)
        created-by-user-id (utils/api-key-user-id ctx)
        {:keys [request-body]} ctx
        form-to-create (assoc request-body
                              :old-slug old-slug
                              :created-by-user-id created-by-user-id)
        created-form (try
                       (db.forms/create-form db-conn form-to-create)
                       (catch Exception e
                         (throw (errors/error-code->ex-info
                                 :form-creation-internal-error
                                 {:form-to-create form-to-create}
                                 e))))]
    {:status 201
     :headers {}
     :body (-> created-form
               utils/declojurify-form)}))
