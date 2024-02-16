(ns dvb.server.http.operations.create-form
  (:require [dvb.common.edges.forms :as form-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.forms :as db.forms]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log]))

(defn- create-form [database form-to-create]
  (let [data {:entity-type :form
              :entity-to-create form-to-create}
        throw-500 (fn [e] (throw (errors/error-code->ex-info
                                  :entity-creation-internal-error
                                  data e)))]
    (try
      (db.forms/create-form database (form-edges/clj->pg form-to-create))
      (catch Exception e (throw-500 e)))))

(defn handle [{:keys [database]}
              {:as ctx form-write :request-body}]
  (log/info "Creating a form.")
  (authorize/authorize ctx)
  (let [old-slug (-> ctx :path :old_slug)
        created-by (utils/security-user-id ctx)
        response {:status 201
                  :headers {}
                  :body (->> (assoc form-write
                                    :old-slug old-slug
                                    :created-by created-by
                                    :updated-by created-by)
                             form-edges/api->clj
                             (create-form database)
                             form-edges/clj->api)}]
    (log/info "Created a form.")
    response))
