(ns dvb.server.http.operations.reject-old-access-request
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.edges.old-access-requests :as old-access-request-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.old-access-requests :as db.old-access-requests]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn- authorize
  "We can reject an OLD access request if the OLD no longer exists. If it does
  exist, then the requesting user must be an admin of the target OLD."
  [old {:as _authenticated-user authenticated-user-id :id :keys [is-superuser?]}]
  (when old ;; If there is no OLD anymore, the request is authorized cuz it's vacuous
    (let [admin-ids (db.olds/old-admin-ids old)]
      (when-not (or is-superuser? (some #{authenticated-user-id} admin-ids))
        (let [message "Only admins can reject an OLD access request"
              data {:message message
                    :authenticated-user-id authenticated-user-id
                    :old-admin-ids admin-ids
                    :error-code :only-admins-can-reject-old-access-requests
                    :operation :reject-old-access-request}]
          (log/warn message data)
          (throw (errors/error-code->ex-info :unauthorized data)))))))

(defn- validate
  "An OLD access request can only be rejected if the request exists."
  [oar oar-id old-slug user-id]
  (let [shared-ctx {:old-access-request-id oar-id
                    :old-slug old-slug
                    :user-id user-id
                    :operation :reject-old-access-request}]
    (when-not oar
      (let [message "OLD access request not found"
            data (merge shared-ctx {:message message
                                    :error-code :no-such-old-access-request})]
        (log/warn data)
        (throw (errors/error-code->ex-info :entity-not-found data))))))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {old-access-request-id :old_access_request_id} :path}]
  (let [old-access-request-id (UUID/fromString old-access-request-id)
        {:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        log-ctx {:authenticated-user-id authenticated-user-id
                 :old-access-request-id old-access-request-id}]
    (log/info "Rejecting an OLD access request." log-ctx)
    (authorize/authorize ctx)
    (jdbc/with-db-transaction [tx database {:isolation :repeatable-read}]
      (let [{:as old-access-request :keys [old-slug user-id]}
            (db.old-access-requests/get-old-access-request
             tx old-access-request-id)
            old (db.olds/get-old-with-users tx old-slug)]
        (validate old-access-request old-access-request-id old-slug user-id)
        (authorize old authenticated-user)
        (let [rejected-request (db.old-access-requests/reject
                                tx old-access-request-id)]
          (log/info "Rejected an OLD access request." log-ctx)
          {:status 200
           :headers {}
           :body (old-access-request-edges/clj->api rejected-request)})))))
