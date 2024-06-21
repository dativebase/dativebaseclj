(ns dvb.server.http.operations.reset-password
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.edges.users :as user-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.users :as db.users]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn- validate [{:as _user-password-reset :keys [secret-key]}
                 {:as existing-user user-id :id :keys [registration-key]}]
  (when-not existing-user
    (throw (errors/error-code->ex-info
            :entity-not-found
            {:entity-type :user
             :entity-id user-id
             :operation :reset-password})))
  (when-not (= registration-key secret-key)
    (throw (errors/error-code->ex-info
            :secret-key-invalid
            {:secret-key secret-key
             :entity-type :user
             :entity-id user-id
             :operation :reset-password}))))

(defn handle [{:keys [database]}
              {:as _ctx user-password-reset :request-body
               {user-id :user_id} :path}]
  (let [user-id (UUID/fromString user-id)
        log-ctx {:user-id user-id}]
    (log/info "Resetting a user password." log-ctx)
    (jdbc/with-db-transaction [tx database {:isolation :repeatable-read}]
      (let [{:as user-password-reset new-password :password}
            (user-edges/user-password-reset-api->clj user-password-reset)
            existing-user (db.users/get-user tx user-id)]
        (validate user-password-reset existing-user)
        (db.users/reset-password tx user-id new-password)
        (log/info "Reset a user password." log-ctx)
        {:status 200
         :headers {}
         :body (user-edges/clj->api existing-user)}))))
