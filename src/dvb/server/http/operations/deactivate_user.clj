(ns dvb.server.http.operations.deactivate-user
  (:require [dvb.common.edges.users :as user-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.db.users :as db.users]
            [dvb.server.entities.users :as users]
            [dvb.server.log :as log]))

(defn- validate [{:as user user-id :id :keys [registration-status]}]
  (when-not user
    (let [message "User not found during deactivation attempt"
          data {:message message
                :entity-type :user
                :entity-id user-id
                :operation :deactivate-user}]
      (log/warn message data)
      (throw (errors/error-code->ex-info :entity-not-found data))))
  (when-not (= :registered registration-status)
    (let [message "User deactivation failed. User is not currently registered."
          data {:message message
                :entity-type :user
                :registration-status registration-status
                :entity-id user-id
                :operation :deactivate-user}]
      (log/warn message data)
      (throw (errors/error-code->ex-info :user-deactivation-failed data)))))

(defn authorize
  [{:as _authenticated-user :keys [is-superuser?] authenticated-user-id :id}
   {:as _target-user target-user-id :id}]
  (when-not (or is-superuser? (= authenticated-user-id target-user-id))
    (let [message "User deactivation can only be performed by the user itself, or a superuser."
          data {:message message
                :authenticated-user-id authenticated-user-id
                :user-id target-user-id}]
      (log/warn message data)
      (throw (errors/error-code->ex-info :unauthorized data)))))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {user-id :user_id} :path}]
  (let [{:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        log-ctx {:user-id user-id
                 :authenticated-user-id authenticated-user-id}]
    (log/info "Deactivating a user." log-ctx)
    (let [user (db.users/get-user-with-plans-and-olds database user-id)]
      (validate user)
      (authorize authenticated-user user)
      (let [deactivated-user
            (try (users/deactivate database user-id authenticated-user-id)
                 (catch Exception e
                   (let [{:as data :keys [error-code]} (ex-data e)]
                     (if (some #{error-code} [:cannot-deactivate-manager
                                              :cannot-deactivate-administrator])
                       (throw (errors/error-code->ex-info :user-deactivation-failed data))
                       (throw (errors/error-code->ex-info :unexpected-error data))))))]
        {:status 200
         :headers {}
         :body (user-edges/clj->api deactivated-user)}))))
