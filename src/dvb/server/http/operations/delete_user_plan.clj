(ns dvb.server.http.operations.delete-user-plan
  (:require [dvb.common.edges.user-plans :as user-plan-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.db.user-plans :as db.user-plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn handle [{:keys [database]}
              {:as ctx {user-plan-id :user_plan_id} :path}]
  (let [user-plan-id (UUID/fromString user-plan-id)
        {:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)]
    (log/info "Deleting a user plan.")
    (authorize/authorize ctx)
    (let [existing-user-plan (db.user-plans/get-user-plan database user-plan-id)
          plan (db.plans/get-plan-with-members database
                                               (:plan-id existing-user-plan))]
      (when-not existing-user-plan
        (throw (errors/error-code->ex-info
                :entity-not-found
                {:entity-type :user-plan
                 :entity-id user-plan-id
                 :operation :delete-user-plan})))
      (utils/validate-plan-role-transition (:role existing-user-plan) nil plan)
      (authorize/authorize-mutate-plan
       :delete-user-plan plan authenticated-user)
      (try
        {:status 200
         :headers {}
         :body (user-plan-edges/clj->api (db.user-plans/delete-user-plan
                                          database
                                          {:id (:id existing-user-plan)
                                           :updated-by authenticated-user-id}))}
        (catch Exception e
          (throw (errors/error-code->ex-info
                  :entity-deletion-internal-error
                  {:entity-type :user-plan
                   :entity-id user-plan-id}
                  e)))))))
