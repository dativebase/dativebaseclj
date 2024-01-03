(ns dvb.server.http.operations.update-old
  (:require [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]
            [dvb.server.http.operations.utils :as utils]))

(defn handle [{:keys [database]}
              {:as ctx old-update :request-body {old-slug :old_slug} :path}]
  (log/info "Updating an OLD.")
  (authorize/authorize ctx)
  (let [old-update (edges/old-api->clj old-update)
        existing-old (db.olds/get-old database old-slug)
        updated-by (utils/security-user-id ctx)]
    (when-not existing-old
      (throw (errors/error-code->ex-info
              :entity-not-found
              {:entity-type :old
               :old-slug old-slug
               :operation :update-old})))
    (when (= old-update (select-keys existing-old (keys old-update)))
      (throw (errors/error-code->ex-info
              :no-changes-in-update
              {:request-payload old-update
               :entity-type :old
               :old-slug old-slug
               :operation :update-old})))
    (try
      {:status 200
       :headers {}
       :body (edges/old-clj->api
              (db.olds/update-old database
                                  (merge existing-old
                                         old-update
                                         {:updated-by updated-by})))}
      (catch Exception e
        (throw (errors/error-code->ex-info
                :entity-update-internal-error
                {:entity-type :old
                 :entity-to-update old-update}
                e))))))
