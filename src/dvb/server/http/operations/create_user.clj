(ns dvb.server.http.operations.create-user
  (:require [clojure.string :as str]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.http.operations.utils.clojurify :as clojurify]
            [dvb.server.http.operations.utils.declojurify :as declojurify]
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

(defn handle [{:keys [database]}
              {:as ctx user-write :request-body}]
  (let [created-by (utils/security-user-id ctx)]
    (log/info "Creating a user.")
    (authorize/authorize ctx)
    (let [response {:status 201
                    :headers {}
                    :body (->> (assoc user-write
                                      :created-by created-by
                                      :updated-by created-by)
                               clojurify/user
                               (create-user database)
                               declojurify/user)}]
      (log/info "Created a user.")
      response)))
