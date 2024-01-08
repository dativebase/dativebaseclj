(ns dvb.server.http.operations.update-user-plan
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.common.edges :as edges]
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

(defn- validate-role-transition
  "You can't un-manager a user from a plan when that user is that plan's last
  manager."
  [from-role to-role plan-members]
  (let [managers (db.plans/plan-managers plan-members)]
    (when (and (= :manager from-role)
               (not= :manager to-role)
               (<= 1 (count managers)))
      (let [message "Refusing to leave a plan without at least one manager. Please assign another user the manager role on this plan before retrying this request."
            data {:message message
                  :managers managers}]
        (log/warn message data)
        (throw (errors/error-code->ex-info
                :entity-creation-internal-error
                data))))))

(defn handle [{:keys [database]}
              {:as ctx
               {:as user-plan-update} :request-body
               {user-plan-id :user_plan_id} :path}]
  (log/info "Updating a user plan.")
  (authorize/authorize ctx)
  (let [user-plan-id (UUID/fromString user-plan-id)
        user-plan-update (edges/user-plan-api->clj user-plan-update)
        {:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        user-plan (db.user-plans/get-user-plan database user-plan-id)
        plan (db.plans/get-plan-with-members database (:plan-id user-plan))]
    (utils/validate-mutate-user-plan
     :update-user-plan database user-plan plan)
    (validate-role-transition (:role user-plan)
                              (:role user-plan-update)
                              (:members plan))
    (authorize/authorize-mutate-plan
     :update-user-plan plan authenticated-user)
    (let [update-fn (partial update-user-plan database)
          response {:status 200
                    :headers {}
                    :body (-> user-plan-update
                              (assoc :id user-plan-id
                                     :updated-by authenticated-user-id)
                              update-fn
                              edges/user-plan-clj->api)}]
      (log/info "Updated a user plan.")
      response)))
