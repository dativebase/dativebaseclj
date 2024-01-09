(ns dvb.server.http.operations.update-user-old
  (:require [clojure.string :as str]
            [dvb.common.openapi.errors :as errors]
            [dvb.common.edges :as edges]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.user-olds :as db.user-olds]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (org.postgresql.util PSQLException)))

(defn- validate [user-old-update {:as existing-user-old user-old-id :id}]
  (when-not existing-user-old
    (throw (errors/error-code->ex-info
            :entity-not-found
            {:entity-type :user-old
             :entity-id user-old-id
             :operation :update-user-old})))
  (when (= user-old-update
           (select-keys existing-user-old (keys user-old-update)))
    (throw (errors/error-code->ex-info
            :no-changes-in-update
            {:request-payload user-old-update
             :entity-type :user-old
             :entity-id user-old-id
             :operation :update-user-old}))))

(defn handle [{:keys [database]}
              {:as ctx
               {:as user-old-update} :request-body
               {user-old-id :user_old_id} :path}]
  (let [{:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        log-ctx {:authenticated-user-id authenticated-user-id
                 :user-old-id user-old-id}]
    (log/info "Updating a user OLD." log-ctx)
    (authorize/authorize ctx)
    (let [user-old-update (edges/user-old-api->clj user-old-update)
          {:as existing-user-old :keys [old-slug]} (db.user-olds/get-user-old
                                                    database user-old-id)
          old (db.olds/get-old-with-users database old-slug)]
      (validate user-old-update existing-user-old)
      (authorize/authorize-mutate-old :update-user-old old authenticated-user)
      (let [update-fn (partial db.user-olds/update-user-old database)
            response {:status 200
                      :headers {}
                      :body (-> user-old-update
                                (assoc :updated-by authenticated-user-id
                                       :id user-old-id)
                                update-fn
                                edges/user-old-clj->api)}]
        (log/info "Updated a user OLD." log-ctx)
        response))))
