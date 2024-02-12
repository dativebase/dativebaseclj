(ns dvb.server.http.operations.create-user
  (:require [clojure.string :as str]
            [dvb.common.edges.users :as user-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log]
            [dvb.server.system.clock :as clock]
            [java-time.api :as jt])
  (:import (org.postgresql.util PSQLException)))

(defn- create-user [database user-to-create]
  (let [data {:entity-type :user
              :entity-to-create user-to-create}
        throw-500 (fn [e] (throw (errors/error-code->ex-info
                                  :entity-creation-internal-error
                                  data e)))]
    (try
      (db.users/create-user database user-to-create)
      (catch PSQLException e
        (if (str/includes? (ex-message e) "users_email_key")
          (throw (errors/error-code->ex-info
                  :unique-email-constraint-violated
                  data))
          (throw-500 e)))
      (catch Exception e (throw-500 e)))))

(defn authorize
  [{:as _user-write new-user-is-superuser? :is-superuser?}
   {:as authenticated-user authenticated-user-is-superuser? :is-superuser?}]
  (when (and new-user-is-superuser?
             (not authenticated-user-is-superuser?))
    (let [message "Authenticated user is not authorized to create a new superuser"
          data {:message message
                :authenticated-user-id (:id authenticated-user)
                :authenticated-user-is-superuser? authenticated-user-is-superuser?
                :to-be-created-user-is-superuser? new-user-is-superuser?
                :operation-id :create-user}]
      (log/warn message data)
      (throw (errors/error-code->ex-info :unauthorized data)))))

;; 12h between user creations
(def minimum-seconds-between-anonymous-user-creation (* 12 60 60))

(defn- rate-limit [database clock remote-addr]
  (when-let [other-user (db.users/most-recent-user-created-by-ip-address
                         database remote-addr)]
    (let [now (clock/now clock)
          other-user-created-at (:created-at other-user)
          seconds-since-last-creation
          (jt/as (jt/duration other-user-created-at now) :seconds)]
      (when (< seconds-since-last-creation
               minimum-seconds-between-anonymous-user-creation)
        (let [message "Anonymous user creation blocked by rate limiting."
              data {:message message
                    :ip-address remote-addr
                    :last-user-id-created-by-this-ip (:id other-user)
                    :last-user-ip-created-by-this-ip (:created-by-ip-address other-user)
                    :last-user-created-at (:created-at other-user)
                    :seconds-since-last-creation seconds-since-last-creation
                    :retry-after minimum-seconds-between-anonymous-user-creation
                    :minimum-seconds-between-anonymous-user-creation minimum-seconds-between-anonymous-user-creation
                    :operation-id :create-user}]
          (log/warn message data)
          (let [error-code :too-many-requests
                response-429 (assoc-in
                              (errors/error-code->response error-code)
                              [:body :errors 0 :retry-after]
                              (* minimum-seconds-between-anonymous-user-creation 60 60))]
            (throw (ex-info (errors/error-code->message error-code)
                            response-429))))))))

(defn handle [{:keys [database clock]}
              {:as ctx user-write :request-body :keys [request]}]
  (let [remote-addr (utils/remote-addr request)
        user-write (user-edges/api->clj user-write)
        {:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)]
    (log/info "Creating a user.")
    (authorize/authorize ctx)
    (authorize user-write authenticated-user)
    (when-not authenticated-user-id (rate-limit database clock remote-addr))
    (let [creator (partial create-user database)
          response {:status 201
                    :headers {}
                    :body (-> user-write
                              (assoc :created-by authenticated-user-id
                                     :updated-by authenticated-user-id
                                     :created-by-ip-address (or remote-addr "unknown"))
                              creator
                              user-edges/clj->api)}]
      (log/info "Created a user.")
      response)))
