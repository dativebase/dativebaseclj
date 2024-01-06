(ns dvb.server.http.operations.create-user
  (:require [clojure.string :as str]
            [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
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

(defn handle [{:keys [database]}
              {:as ctx user-write :request-body}]
  (let [user-write (edges/user-api->clj user-write)
        {:as authenticated-user authenticated-user-id :id}
         (utils/security-user ctx)]
    (log/info "Creating a user.")
    (authorize/authorize ctx)
    (authorize user-write authenticated-user)
    (let [creator (partial create-user database)
          response {:status 201
                    :headers {}
                    :body (-> user-write
                              (assoc :created-by authenticated-user-id
                                     :updated-by authenticated-user-id)
                              creator
                              edges/user-clj->api)}]
      (log/info "Created a user.")
      response)))
