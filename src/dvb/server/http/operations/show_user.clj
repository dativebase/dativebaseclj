(ns dvb.server.http.operations.show-user
  (:require [dvb.common.edges :as edges]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {user-id :user_id} :path}]
  (log/info "Showing a user." {:user-id user-id})
  (authorize/authorize ctx)
  (if-let [user (db.users/get-user database user-id)]
    {:status 200
     :headers {}
     :body (edges/user-clj->api user)}
    {:status 404
     :headers {}
     :body
     {:errors
      [{:message "The referenced user could not be found. Please ensure that the supplied identifier is correct."
        :error-code "entity-not-found"}]}}))
