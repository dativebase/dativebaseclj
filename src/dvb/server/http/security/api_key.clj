(ns dvb.server.http.security.api-key
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.server.db.api-keys :as db.api-keys]
            [dvb.server.db.users :as db.users]
            [dvb.server.encrypt :as encrypt]
            [taoensso.timbre :as log])
  (:import (java.util UUID)))

(defn handle
  [{:as _system :keys [database]} _ctx {:as x :keys [x-app-id x-api-key]}]
  (log/info "API key authentication attempt.")
  (when-not x-app-id
    (log/warn "No X-APP-ID value was supplied in the request.")
    (throw (errors/error-code->ex-info :unauthenticated)))
  (when-not x-api-key
    (log/warn "No X-API-KEY value was supplied in the request.")
    (throw (errors/error-code->ex-info :unauthenticated)))
  (let [api-key-id (try (UUID/fromString x-app-id)
                        (catch Exception _
                          (log/warn "The supplied X-APP-ID value is not a valid UUID string.")
                          (throw (errors/error-code->ex-info :unauthenticated))))
        api-key
        (try (db.api-keys/get-api-key database api-key-id)
             (catch Exception e
               (log/error e
                          "Exception thrown when attempting to query api_keys based on X-APP-ID"
                          {:x-app-id x-app-id})
               (throw (errors/error-code->ex-info :unauthenticated))))]
    (when-not api-key
      (log/warn "Unable to locate the referenced API key."
                {:x-app-id x-app-id})
      (throw (errors/error-code->ex-info :unauthenticated)))
    (when-not (= x-api-key (:key api-key))
      (log/warn "Invalid key supplied for the referenced API key."
                {:x-app-id x-app-id})
      (throw (errors/error-code->ex-info :unauthenticated)))
    (let [user (db.users/get-user-with-roles database (:user-id api-key))
          ret {:authenticated? true
               :user user}]
      (log/info "Successful API key authentication." {:user-id (:id user)})
      ret)))
