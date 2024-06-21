(ns dvb.server.http.operations.retract-old-access-request
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.edges.old-access-requests :as old-access-request-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.old-access-requests :as db.old-access-requests]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn- authorize
  "Only the user referenced in the OLD access request can retract said request."
  [{:as old-access-request :keys [user-id]}
   {:as _authenticated-user authenticated-user-id :id :keys [is-superuser?]}]
  (when-not (or is-superuser? (= authenticated-user-id user-id))
    (let [message "Only the target user of an OLD access request can retract it."
          data {:message message
                :authenticated-user-id authenticated-user-id
                :target-user-id user-id
                :old-access-request-id (:id old-access-request)
                :operation :retract-old-access-request
                :error-code :only-target-can-retract-access-request}]
      (log/warn message data)
      (throw (errors/error-code->ex-info :unauthorized data)))))

(defn- validate
  "An OLD access request can only be retracted if the request exists."
  [oar oar-id]
  (when-not oar
    (let [message "OLD access request not found"
          data {:message message
                :error-code :no-such-old-access-request
                :old-access-request-id oar-id
                :operation :retract-old-access-request}]
      (log/warn data)
      (throw (errors/error-code->ex-info :entity-not-found data)))))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {old-access-request-id :old_access_request_id} :path}]
  (let [old-access-request-id (UUID/fromString old-access-request-id)
        {:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        log-ctx {:authenticated-user-id authenticated-user-id
                 :old-access-request-id old-access-request-id}]
    (log/info "Retracting an OLD access request." log-ctx)
    (authorize/authorize ctx)
    (jdbc/with-db-transaction [tx database {:isolation :repeatable-read}]
      (let [old-access-request (db.old-access-requests/get-old-access-request
                                tx old-access-request-id)]
        (validate old-access-request old-access-request-id)
        (authorize old-access-request authenticated-user)
        (let [retracted-request (db.old-access-requests/retract
                                tx old-access-request-id)]
          (log/info "Retracted an OLD access request." log-ctx)
          {:status 200
           :headers {}
           :body (old-access-request-edges/clj->api retracted-request)})))))
