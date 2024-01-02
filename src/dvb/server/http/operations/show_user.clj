(ns dvb.server.http.operations.show-user
  (:require [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as u]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {user-id :user_id} :path
    {include-plans? :include-plans} :query}]
  (let [user-id (UUID/fromString user-id)]
    (log/info "Showing a user." {:user-id user-id
                                 :include-plans? include-plans?})
    (authorize/authorize ctx)
    (let [{:keys [is-superuser?] authenticated-user-id :id}
          (u/security-user ctx)]
      (when-not (or is-superuser?
                    (= authenticated-user-id user-id))
        (log/warn "Request to show user is unauthorized."
                  {:user-id user-id
                   :authenticated-user-id authenticated-user-id
                   :is-superuser? is-superuser?
                   :operation-id :show-user})
        (throw (errors/error-code->ex-info :unauthorized))))
    (let [user (if include-plans?
                 (db.users/get-user-with-plans database user-id)
                 (db.users/get-user database user-id))]
      (if user
        {:status 200
         :headers {}
         :body (edges/user-clj->api user)}
        {:status 404
         :headers {}
         :body
         {:errors
          [{:message "The referenced user could not be found. Please ensure that the supplied identifier is correct."
            :error-code "entity-not-found"}]}}))))
