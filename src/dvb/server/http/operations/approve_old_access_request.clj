(ns dvb.server.http.operations.approve-old-access-request
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.common.edges.old-access-requests :as old-access-request-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.old-access-requests :as db.old-access-requests]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.users :as db.users]
            [dvb.server.db.user-olds :as db.user-olds]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn- authorize
  [old {:as _authenticated-user authenticated-user-id :id :keys [is-superuser?]}]
  (let [admin-ids (db.olds/old-admin-ids old)]
    (when-not (or is-superuser? (some #{authenticated-user-id} admin-ids))
      (let [message "Only admins can approve an OLD access request"
            data {:message message
                  :authenticated-user-id authenticated-user-id
                  :old-admin-ids admin-ids
                  :error-code :only-admins-can-approve-old-access-requests
                  :operation :approve-old-access-request}]
        (log/warn message data)
        (throw (errors/error-code->ex-info :unauthorized data))))))

(defn- validate
  "An OLD access request can only be approved if the request exists, the OLD
  exists and the user exists."
  [oar old user oar-id old-slug user-id]
  (let [shared-ctx {:old-access-request-id oar-id
                    :old-slug old-slug
                    :user-id user-id
                    :operation :approve-old-access-request}]
    (when-not oar
      (let [message "OLD access request not found"
            data (merge shared-ctx {:message message
                                    :error-code :no-such-old-access-request})]
        (log/warn data)
        (throw (errors/error-code->ex-info :entity-not-found data))))
    (when-not old
      (let [message "Attempted to approve an access request for an OLD that doees not exist."
            data (merge shared-ctx {:message message
                                    :error-code :old-access-request-approval-on-nonexistent-old})]
        (log/warn message data)
        (throw (errors/error-code->ex-info :entity-not-found data))))
    (when-not user
      (let [message "Attempted to approve an access request for a user that doees not exist."
            data (merge shared-ctx {:message message
                                    :error-code :old-access-request-approval-on-nonexistent-user})]
        (log/warn message data)
        (throw (errors/error-code->ex-info :entity-not-found data))))))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {old-access-request-id :old_access_request_id} :path}]
  (let [old-access-request-id (UUID/fromString old-access-request-id)
        {:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        log-ctx {:authenticated-user-id authenticated-user-id
                 :old-access-request-id old-access-request-id}]
    (log/info "Approving an OLD access request." log-ctx)
    (authorize/authorize ctx)
    (jdbc/with-db-transaction [tx database {:isolation :repeatable-read}]
      (let [{:as old-access-request :keys [old-slug user-id]}
            (db.old-access-requests/get-old-access-request
             tx old-access-request-id)
            old (db.olds/get-old-with-users tx old-slug)
            user (db.users/get-user tx user-id)]
        (validate old-access-request old user old-access-request-id old-slug
                  user-id)
        (authorize old authenticated-user)
        (let [approved-request (db.old-access-requests/approve
                                tx old-access-request-id)]
          (if (some #{user-id} (db.olds/old-user-ids old))
            (log/warn "User is already a member of the OLD. OLD access request is approved but no access has been granted since it is already present."
                      (merge log-ctx {:user-id user-id
                                      :old-user-ids (db.olds/old-user-ids old)}))
            (db.user-olds/create-user-old
             tx {:user-id user-id
                 :old-slug old-slug
                 :role :viewer
                 :created-by authenticated-user-id
                 :updated-by authenticated-user-id}))
          {:status 200
           :headers {}
           :body (old-access-request-edges/clj->api approved-request)})))))
