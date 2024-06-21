(ns dvb.server.http.operations.initiate-password-reset
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log]
            [dvb.server.system.email :as email]))

(defn- validate [{:as user user-id :id :keys [registration-status]}]
  (when-not user
    (let [message "User not found during attempt to initiate a password reset."
          data {:message message
                :entity-type :user
                :entity-id user-id
                :operation :initiate-password-reset}]
      (log/warn message data)
      (throw (errors/error-code->ex-info :entity-not-found data))))
  (when-not (= :registered registration-status)
    (let [message "Attempt to initiate a password reset failed because the target user is not yet registered."
          data {:message message
                :entity-type :user
                :registration-status registration-status
                :entity-id user-id
                :operation :initiate-password-reset}]
      (log/warn message data)
      (throw (errors/error-code->ex-info :initiate-password-reset-failed data)))))

(def password-reset-email-template
  (str "Hello,\n\n"
       "DativeBase has received a request to reset the password for the user"
       " with email address %s.\n\n"
       "This is the secret key that you need in order to reset your password:\n\n"
       "  %s\n\n"
       "Thanks for using DativeBase,\n\n"
       "The DativeBase Team"))

;; WARNING / TODO: This does not actually currently send an email.
(defn- send-password-reset-email [email-component email-address registration-key]
  (email/send
   email-component
   email-address
   (format password-reset-email-template email-address registration-key)))

(defn handle
  [{:as _application :keys [database] email-component :email}
   {:as ctx {user-id :user_id} :path}]
  (let [{:as _authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        log-ctx {:user-id user-id
                 :authenticated-user-id authenticated-user-id}]
    (log/info "Initiating a password reset." log-ctx)
    (let [user (db.users/get-user-with-plans-and-olds database user-id)]
      (validate user)
      (let [{:as _user-with-refreshed-registration-key
             :keys [registration-key email]}
            (db.users/refresh-registration-key database user-id)]
        (send-password-reset-email email-component email registration-key)
        (log/info "Initiated a password reset." log-ctx)
        {:status 204
         :headers {}}))))
