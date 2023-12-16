(ns dvb.server.http.operations.show-user
  (:require [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils.declojurify :as declojurify]
            [taoensso.timbre :as log]))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {old-slug :old_slug user-id :user_id} :path}]
  (log/info "Showing a user." {:user-id user-id :old-slug old-slug})
  (authorize/authorize ctx)
  (if-let [user (db.users/get-user database user-id)]
    {:status 200
     :headers {}
     :body (-> user
               (select-keys [:id
                             :first-name
                             :last-name
                             :email
                             :created-at
                             :updated-at
                             :destroyed-at])
               declojurify/user)}
    {:status 404
     :headers {}
     :body
     {:errors
      [{:message "The referenced user could not be found. Please ensure that the supplied identifier is correct."
        :error-code "entity-not-found"}]}}))
