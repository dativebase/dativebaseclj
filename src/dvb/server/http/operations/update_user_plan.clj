(ns dvb.server.http.operations.update-user-plan
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.common.edges.user-plans :as user-plan-edges]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.db.user-plans :as db.user-plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn- update-user-plan [database user-plan-to-update]
  (let [data {:entity-type :user-plan
              :entity-to-update user-plan-to-update}
        throw-500 (fn [e] (throw (errors/error-code->ex-info
                                  :entity-creation-internal-error
                                  data e)))]
    (try
      (db.user-plans/update-user-plan database user-plan-to-update)
      (catch Exception e (throw-500 e)))))

(defn handle [{:keys [database]}
              {:as ctx
               {:as user-plan-update} :request-body
               {user-plan-id :user_plan_id} :path}]
  (log/info "Updating a user plan.")
  (authorize/authorize ctx)
  (let [user-plan-id (UUID/fromString user-plan-id)
        user-plan-update (user-plan-edges/api->clj user-plan-update)
        {:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        existing-user-plan (db.user-plans/get-user-plan database user-plan-id)
        plan (db.plans/get-plan-with-members database (:plan-id existing-user-plan))]
    (utils/validate-mutate-user-plan
     :update-user-plan database existing-user-plan plan)
    (utils/validate-plan-role-transition (:role existing-user-plan)
                              (:role user-plan-update)
                              plan)
    (authorize/authorize-mutate-plan
     :update-user-plan plan authenticated-user)
    (let [update-fn (partial update-user-plan database)
          response {:status 200
                    :headers {}
                    :body (-> user-plan-update
                              (assoc :id user-plan-id
                                     :updated-by authenticated-user-id)
                              update-fn
                              user-plan-edges/clj->api)}]
      (log/info "Updated a user plan.")
      response)))
