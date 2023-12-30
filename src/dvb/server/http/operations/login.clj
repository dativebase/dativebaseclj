(ns dvb.server.http.operations.login
  (:require [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.users :as db.users]
            [dvb.server.db.api-keys :as db.api-keys]
            [dvb.server.encrypt :as encrypt]
            [dvb.server.system.clock :as clock]
            [java-time.api :as jt]
            [dvb.server.log :as log]))

(def api-key-duration-hours 2)

(defn- generate-api-key-for-user [user now]
  {:user-id (:id user)
   :key (str (random-uuid))
   :expires-at (jt/plus now (jt/hours api-key-duration-hours))})

(defn handle [{:as _system :keys [database clock]}
              {{{:keys [email password]} :body} :request :as _ctx}]
  (log/info "Login attempt." {:email email})
  (let [user (db.users/get-user-by-email database email)
        now (clock/now clock)]
    (when-not user
      (log/warn "Login attempt with an unrecognized email address."
                {:email email})
      (throw (errors/error-code->ex-info :unauthenticated)))
    (when-not (encrypt/checkpw password (:password user))
      (log/warn "Login attempt with an invalid password."
                {:email email})
      (throw (errors/error-code->ex-info :unauthenticated)))
    (when-not (= :registered (:registration-status user))
      (log/warn "Login attempt with an unregistered user. User must move from pending to registered in order to authenticate."
                {:email email
                 :registration-status (:registration-status user)})
      (throw (errors/error-code->ex-info :unregistered-user)))
    (let [api-key (db.api-keys/create-api-key
                   database (generate-api-key-for-user user now))]
      (log/info "Login succeeded." {:email email :user-id (:id user)})
      {:status 200
       :headers {}
       :body {:user (edges/user-clj->api user)
              :api-key (edges/api-key-clj->api api-key)}})))
