(ns dvb.server.http.operations.delete-user
  (:require [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn handle [{:keys [database]}
              {:as ctx {user-id :user_id} :path
               {{authenticated-user-id :id} :user} :security}]
  (let [user-id (UUID/fromString user-id)
        updated-by (utils/security-user-id ctx)]
    (log/info "Deleting a user.")
    (authorize/authorize ctx)
    (when (= authenticated-user-id user-id)
      (throw (errors/error-code->ex-info
              :deletion-request-for-current-user
              {:user-id user-id
               :authenticated-user-id authenticated-user-id})))
    (let [existing-user (db.users/get-user database user-id)]
      (when-not existing-user
        (throw (errors/error-code->ex-info
                :entity-not-found
                {:entity-type :user
                 :entity-id user-id
                 :operation :delete-user})))
      (try
        {:status 200
         :headers {}
         :body (edges/user-clj->api (db.users/delete-user
                                     database
                                     {:id (:id existing-user)
                                      :updated-by updated-by}))}
        (catch Exception e
          (throw (errors/error-code->ex-info
                  :entity-deletion-internal-error
                  {:entity-type :user
                   :entity-id user-id}
                  e)))))))
