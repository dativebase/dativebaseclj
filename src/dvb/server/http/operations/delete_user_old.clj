(ns dvb.server.http.operations.delete-user-old
  (:require [dvb.common.edges.user-olds :as user-old-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.user-olds :as db.user-olds]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn handle [{:keys [database]}
              {:as ctx {user-old-id :user_old_id} :path}]
  (let [user-old-id (UUID/fromString user-old-id)
        {:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        log-ctx {:authenticated-user-id authenticated-user-id
                 :user-old-id user-old-id}]
    (log/info "Deleting a user old." log-ctx)
    (authorize/authorize ctx)
    (let [existing-user-old (db.user-olds/get-user-old database user-old-id)
          old (db.olds/get-old-with-users database (:old-slug existing-user-old))]
      (when-not existing-user-old
        (throw (errors/error-code->ex-info
                :entity-not-found
                {:entity-type :user-old
                 :entity-id user-old-id
                 :operation :delete-user-old})))
      (authorize/authorize-mutate-old
       :delete-user-old old authenticated-user)
      (utils/validate-old-role-transition (:role existing-user-old) nil old)
      (try
        {:status 200
         :headers {}
         :body (user-old-edges/clj->api (db.user-olds/delete-user-old
                                          database
                                          {:id (:id existing-user-old)
                                           :updated-by authenticated-user-id}))}
        (catch Exception e
          (throw (errors/error-code->ex-info
                  :entity-deletion-internal-error
                  {:entity-type :user-old
                   :entity-id user-old-id}
                  e)))))))
