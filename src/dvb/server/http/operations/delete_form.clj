(ns dvb.server.http.operations.delete-form
  (:require [dvb.common.edges.forms :as form-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.forms :as db.forms]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log]))

(defn handle [{:keys [database]}
              {:as ctx {old-slug :old_slug form-id :form_id} :path}]
  (log/info "Deleting a form.")
  (authorize/authorize ctx)
  (let [old-slug (keyword old-slug) ;; TODO in edges somehow
        existing-form (db.forms/get-form database form-id)
        authenticated-user-id (utils/security-user-id ctx)]
    (utils/validate-entity-operation
     {:existing-entity existing-form
      :entity-type :form
      :entity-id form-id
      :old-slug old-slug
      :operation :delete-form})
    (try
      {:status 200
       :headers {}
       :body (form-edges/clj->api (db.forms/delete-form
                                   database
                                   {:id (:id existing-form)
                                    :updated-by authenticated-user-id}))}
      (catch Exception e
        (throw (errors/error-code->ex-info
                :entity-deletion-internal-error
                {:entity-type :form
                 :entity-id form-id
                 :old-slug old-slug}
                e))))))
