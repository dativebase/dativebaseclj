(ns dvb.server.http.operations.update-user
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils.clojurify :as clojurify]
            [dvb.server.http.operations.utils.declojurify :as declojurify]
            [dvb.server.log :as log]))

(defn handle [{:keys [database]}
              {:as ctx user-update :request-body {user-id :user_id} :path}]
  (log/info "Updating a user.")
  (authorize/authorize ctx)
  (let [user-update (clojurify/user user-update)
        existing-user (db.users/get-user database user-id)]
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
       :body (declojurify/user
              (db.users/update-user database
                                    (merge existing-user user-update)))}
      (catch Exception e
        (throw (errors/error-code->ex-info
                :entity-update-internal-error
                {:entity-type :user
                 :entity-to-update user-update}
                e))))))
