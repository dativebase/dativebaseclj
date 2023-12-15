(ns dvb.server.http.operations.update-form
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.server.db.forms :as db.forms]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.http.operations.utils.declojurify :as declojurify]
            [taoensso.timbre :as log]))

(defn handle [{:keys [database]}
              {:as ctx
               :keys [request-body]
               {old-slug :old_slug form-id :form_id} :path}]
  (log/info "Updating a form.")
  (authorize/authorize ctx)
  (let [existing-form (db.forms/get-form database form-id)]
    (utils/validate-entity-operation
     {:existing-entity existing-form
      :entity-type :form
      :entity-id form-id
      :old-slug old-slug
      :operation :update-form})
    (when (= request-body (select-keys existing-form (keys request-body)))
      (throw (errors/error-code->ex-info
              :no-changes-in-update
              {:request-payload request-body
               :entity-type :form
               :entity-id form-id
               :operation :update-form})))
    (try
      {:status 200
       :headers {}
       :body (declojurify/form
              (db.forms/update-form database
                                    (merge existing-form request-body)))}
      (catch Exception e
        (throw (errors/error-code->ex-info
                :entity-update-internal-error
                {:entity-type :form
                 :entity-to-update request-body}
                e))))))
