(ns dvb.server.http.operations.update-user
  (:require [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]
            [dvb.server.http.operations.utils :as utils]))

(defn- validate [user-update {:as existing-user user-id :id}]
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
             :operation :update-user}))))

(defn- authorize
  "Only superusers and the user itself can update a user."
  [authenticated-user user]
  (when-not (authorize/authenticated-user-authorized-to-mutate-user?
             authenticated-user user)
    (let [data {:authenticated-user-id (:id authenticated-user)
                :user-id (:id user)
                :operation-id :update-user}]
      (log/warn "Authenticated user is not authorized to updated this user." data)
      (throw (errors/error-code->ex-info :unauthorized data)))))

(defn handle [{:keys [database]}
              {:as ctx user-update :request-body {user-id :user_id} :path}]
  (log/info "Updating a user.")
  (authorize/authorize ctx)
  (let [user-update (edges/user-api->clj user-update)
        existing-user (db.users/get-user database user-id)
        {:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)]
    (validate user-update existing-user)
    (authorize authenticated-user existing-user)
    (try
      {:status 200
       :headers {}
       :body (edges/user-clj->api
              (db.users/update-user database
                                    (merge existing-user
                                           user-update
                                           {:updated-by authenticated-user-id})))}
      (catch Exception e
        (throw (errors/error-code->ex-info
                :entity-update-internal-error
                {:entity-type :user
                 :entity-to-update user-update}
                e))))))
