(ns dvb.server.http.operations.update-user
  (:require [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]
            [dvb.server.http.operations.utils :as utils]))

(defn handle [{:keys [database]}
              {:as ctx user-update :request-body {user-id :user_id} :path}]
  (log/info "Updating a user.")
  (authorize/authorize ctx)
  (let [user-update (edges/user-api->clj user-update)
        existing-user (db.users/get-user database user-id)
        updated-by (utils/security-user-id ctx)]
    (when-not existing-user
      (throw (errors/error-code->ex-info
              :entity-not-found
              {:entity-type :user
               :entity-id user-id
               :operation :update-user})))
    (when (= user-update (select-keys existing-user (keys user-update)))
      (throw (errors/error-code->ex-info
              :no-changes-in-update
              {:request-payload user-update
               :entity-type :user
               :entity-id user-id
               :operation :update-user})))
    (try
      {:status 200
       :headers {}
       :body (edges/user-clj->api
              (db.users/update-user database
                                    (merge existing-user
                                           user-update
                                           {:updated-by updated-by})))}
      (catch Exception e
        (throw (errors/error-code->ex-info
                :entity-update-internal-error
                {:entity-type :user
                 :entity-to-update user-update}
                e))))))
