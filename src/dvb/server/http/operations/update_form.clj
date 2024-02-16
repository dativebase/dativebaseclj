(ns dvb.server.http.operations.update-form
  (:require [dvb.common.edges.forms :as form-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.forms :as db.forms]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log]))

(defn handle [{:keys [database]}
              {:as ctx form-update :request-body
               {old-slug :old_slug form-id :form_id} :path}]
  (let [authenticated-user-id (utils/security-user-id ctx)
        log-ctx {:authenticated-user-id authenticated-user-id
                 :old-slug old-slug}]
    (log/info "Updating a form." log-ctx)
    (authorize/authorize ctx)
    (let [form-update (form-edges/api->clj form-update)
          existing-form (db.forms/get-form database form-id)]
      (utils/validate-entity-operation
       {:existing-entity existing-form
        :entity-type :form
        :entity-id form-id
        :old-slug old-slug
        :operation :update-form})
      (when (= form-update (select-keys existing-form (keys form-update)))
        (throw (errors/error-code->ex-info
                :no-changes-in-update
                {:request-payload form-update
                 :entity-type :form
                 :entity-id form-id
                 :operation :update-form})))
      (try
        (let [response {:status 200
                        :headers {}
                        :body (form-edges/clj->api
                               (db.forms/update-form
                                database
                                (merge existing-form
                                       form-update
                                       {:updated-by authenticated-user-id})))}]
          (log/info "Updated form." log-ctx)
          response)
        (catch Exception e
          (throw (errors/error-code->ex-info
                  :entity-update-internal-error
                  {:entity-type :form
                   :entity-to-update form-update}
                  e)))))))
