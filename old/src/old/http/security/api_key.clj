(ns old.http.security.api-key
  (:require [old.db.users :as db.users]
            [old.encrypt :as encrypt]
            [old.http.openapi.errors :as errors]
            [old.system.db :as system-db]
            [taoensso.timbre :as log]))

(defn handle
  [application _ctx api-key-data]
  (let [database (:database application)
        db-conn (system-db/conn database)
        {:keys [x-app-id x-api-key]} api-key-data]
    (when-not x-app-id
      (log/warn "No X-APP-ID value was supplied in the request.")
      (throw (errors/error-code->ex-info :unauthenticated)))
    (when-not x-api-key
      (log/warn "No X-API-KEY value was supplied in the request.")
      (throw (errors/error-code->ex-info :unauthenticated)))
    (if-let [machine-user
             (try (db.users/get-machine-user db-conn x-app-id)
                  (catch Exception _
                    (log/warn "Exception thrown when attempting to query machine user based on X-APP-ID"
                              {:x-app-id x-app-id})
                    (throw (errors/error-code->ex-info :unauthenticated))))]
      (if (encrypt/checkpw x-api-key (:api-key machine-user))
        (let [user (db.users/get-user-with-roles db-conn (:user-id machine-user))
              ret {:authenticated? true,
                   :user (select-keys user [:id :roles])}]
          (log/info "Successful API key authentication." ret)
          ret)
        (do (log/warn "Invalid API key supplied for machine user."
                      {:x-app-id x-app-id})
            (throw (errors/error-code->ex-info :unauthenticated))))
      (do (log/warn "Unable to locate the referenced machine-user."
                    {:x-app-id x-app-id})
          (throw (errors/error-code->ex-info :unauthenticated))))))
