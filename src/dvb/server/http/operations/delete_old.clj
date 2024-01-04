(ns dvb.server.http.operations.delete-old
  (:require [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log]))

(defn handle [{:keys [database]}
              {:as ctx {old-slug :old_slug} :path}]
  (let [updated-by (utils/security-user-id ctx)]
    (log/info "Deleting a old.")
    (authorize/authorize ctx)
    (let [existing-old (db.olds/get-old database old-slug)]
      (when-not existing-old
        (throw (errors/error-code->ex-info
                :entity-not-found
                {:entity-type :old
                 :old-slug old-slug
                 :operation :delete-old})))
      (try
        {:status 200
         :headers {}
         :body (edges/old-clj->api (db.olds/delete-old
                                     database
                                     {:slug old-slug
                                      :updated-by updated-by}))}
        (catch Exception e
          (throw (errors/error-code->ex-info
                  :entity-deletion-internal-error
                  {:entity-type :old
                   :old-slug old-slug}
                  e)))))))
