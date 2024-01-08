(ns dvb.server.http.operations.create-user-old
  (:require [clojure.string :as str]
            [dvb.common.openapi.errors :as errors]
            [dvb.common.edges :as edges]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.user-olds :as db.user-olds]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (org.postgresql.util PSQLException)))

(defn- create-user-old [database user-old-to-create]
  (let [data {:entity-type :user-old
              :entity-to-create user-old-to-create}
        throw-500 (fn [e] (throw (errors/error-code->ex-info
                                  :entity-creation-internal-error
                                  data e)))]
    (try
      (db.user-olds/create-user-old database user-old-to-create)
      (catch PSQLException e
        (if (str/includes? (ex-message e) "users_olds_unique")
          (throw (errors/error-code->ex-info
                  :users-olds-unique-constraint-violated
                  data))
          (throw-500 e)))
      (catch Exception e (throw-500 e)))))

(defn handle [{:keys [database]}
              {:as ctx {:as user-old-write :keys [old-slug]} :request-body}]
  (log/info "Creating a user OLD.")
  (authorize/authorize ctx)
  (let [user-old-write (edges/user-old-api->clj user-old-write)
        {:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        old (db.olds/get-old-with-users database old-slug)]
    (utils/validate-mutate-user-old
     :create-user-old database user-old-write old)
    (authorize/authorize-mutate-old :create-user-old old authenticated-user)
    (let [create-fn (partial create-user-old database)
          response {:status 201
                    :headers {}
                    :body (-> user-old-write
                              (assoc :created-by authenticated-user-id
                                     :updated-by authenticated-user-id)
                              create-fn
                              edges/user-old-clj->api)}]
      (log/info "Created a user OLD.")
      response)))
