(ns dvb.server.http.operations.create-old-access-request
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.openapi.errors :as errors]
            [dvb.common.edges.old-access-requests :as old-access-request-edges]
            [dvb.server.db.old-access-requests :as db.old-access-requests]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log]))

(defn- create-old-access-request-in-db
  [database old-access-request-to-create]
  (let [data {:entity-type :old-access-request
              :entity-to-create old-access-request-to-create}
        throw-500 (fn [e] (throw (errors/error-code->ex-info
                                  :entity-creation-internal-error
                                  data e)))]
    (try
      (db.old-access-requests/create-old-access-request
       database old-access-request-to-create)
      (catch Exception e (throw-500 e)))))

(defn- validate [existing-requests-for-same-user old user old-slug user-id]
  (let [shared-ctx {:old-slug old-slug :user-id user-id}]
    (when-not old
      (let [message "Attempted to create an access request for an OLD that does not exist."
            data (merge shared-ctx {:message message
                                    :error-code :old-access-request-on-nonexistent-old})]
        (log/warn message data)
        (throw (errors/error-code->ex-info :entity-not-found data))))
    (when-not user
      (let [message "Attempted to create an access request for a user that does not exist."
            data (merge shared-ctx {:message message
                                    :error-code :old-access-request-on-nonexistent-user})]
        (log/warn message data)
        (throw (errors/error-code->ex-info :entity-not-found data))))
    (when-let [requests-for-same-old (seq (for [oar existing-requests-for-same-user
                                                :when (= old-slug (:old-slug oar))]
                                            oar))]
      (let [message "Attempted to create an unnecessary OLD access request. Such a request already exists for this user and OLD."

            data (merge shared-ctx {:message message
                                    :existing-request-id (-> requests-for-same-old first :id)
                                    :error-code :old-access-request-redundant})]
        (log/warn message data)
        (throw (errors/error-code->ex-info :redundant-request data))))
    (when (some #{user-id} (db.olds/old-user-ids old))
      (let [message "Attempted to create an unnecessary OLD access request. The referenced user already has access to the referenced OLD."
            data (merge shared-ctx {:message message
                                    :error-code :old-access-request-redundant})]
        (log/warn message data)
        (throw (errors/error-code->ex-info :redundant-request data))))))

(defn handle
  "Create a new OLD access request."
  [{:keys [database]} {:as ctx old-access-request-write :request-body}]
  (let [{:as old-access-request-write :keys [user-id old-slug]}
        (old-access-request-edges/api->clj old-access-request-write)
        {:as _authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        log-ctx {:authenticated-user-id authenticated-user-id
                 :user-id user-id
                 :old-slug old-slug}]
    (log/info "Creating an OLD access request." log-ctx)
    (authorize/authorize ctx)
    (jdbc/with-db-transaction [tx database {:isolation :repeatable-read}]
      (let [existing-requests-for-same-user
            (db.old-access-requests/get-pending-old-access-requests-for-user
             tx user-id)
            old (db.olds/get-old-with-users tx old-slug)
            user (db.users/get-user tx user-id)]
        (validate existing-requests-for-same-user old user old-slug user-id)
        (let [old-access-request (create-old-access-request-in-db
                                  tx (assoc old-access-request-write
                                            :created-by authenticated-user-id
                                            :updated-by authenticated-user-id))
              response {:status 201
                        :headers {}
                        :body (old-access-request-edges/clj->api
                               old-access-request)}]
          (log/info "Created an OLD access request." log-ctx)
          response)))))
