(ns dvb.server.http.operations.create-form
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.server.db.forms :as db.forms]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.http.operations.utils.declojurify :as declojurify]
            [taoensso.timbre :as log]))

(defn handle [{:keys [database]} ctx]
  (log/info "Creating a form.")
  (authorize/authorize ctx)
  (let [old-slug (-> ctx :path :old_slug)
        created-by-user-id (utils/security-user-id ctx)
        {:keys [request-body]} ctx
        form-to-create (assoc request-body
                              :old-slug old-slug
                              :created-by-user-id created-by-user-id)
        created-form (try
                       (db.forms/create-form database form-to-create)
                       (catch Exception e
                         (throw (errors/error-code->ex-info
                                 :entity-creation-internal-error
                                 {:entity-type :form
                                  :entity-to-create form-to-create}
                                 e))))]
    {:status 201
     :headers {}
     :body (-> created-form
               declojurify/form)}))
