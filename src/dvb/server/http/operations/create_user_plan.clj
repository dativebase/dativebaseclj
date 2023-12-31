(ns dvb.server.http.operations.create-user-plan
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.common.edges :as edges]
            [dvb.server.db.user-plans :as db.user-plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log]))

(defn- create-user-plan [database user-plan-to-create]
  (let [data {:entity-type :user-plan
              :entity-to-create user-plan-to-create}
        throw-500 (fn [e] (throw (errors/error-code->ex-info
                                  :entity-creation-internal-error
                                  data e)))]
    (try
      (db.user-plans/create-user-plan database user-plan-to-create)
      (catch Exception e (throw-500 e)))))

(defn handle [{:keys [database]}
              {:as ctx user-plan-write :request-body}]
  (log/info "Creating a user plan.")
  (authorize/authorize ctx)
  (let [created-by (utils/security-user-id ctx)
        creator (partial create-user-plan database)
        response {:status 201
                  :headers {}
                  :body (-> user-plan-write
                            edges/user-plan-api->clj
                            (assoc :created-by created-by
                                   :updated-by created-by)
                            creator
                            edges/user-plan-clj->api)}]
    (log/info "Created a user plan.")
    response))
