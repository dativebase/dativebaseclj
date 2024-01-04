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
          (u/security-user ctx)
          user (if include-plans?
                 (db.users/get-user-with-plans database user-id)
                 (db.users/get-user database user-id))
          redacted-access? (not (or is-superuser?
                                    (= authenticated-user-id user-id)))]
      (when-not user
        (let [data {:entity-type :user
                    :entity-id user-id
                    :operation :show-user}]
          (log/warn "User not found" data)
          (throw (errors/error-code->ex-info :entity-not-found data))))
      {:status 200
       :headers {}
       :body (cond-> user
               redacted-access? u/minimize-user
               :always edges/user-clj->api)})))
