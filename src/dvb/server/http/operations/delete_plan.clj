(ns dvb.server.http.operations.delete-plan
  (:require [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn handle [{:keys [database]}
              {:as ctx {plan-id :plan_id} :path}]
  (let [plan-id (UUID/fromString plan-id)
        updated-by (utils/security-user-id ctx)]
    (log/info "Deleting a plan.")
    (authorize/authorize ctx)
    (let [existing-plan (db.plans/get-plan database plan-id)]
      (when-not existing-plan
        (throw (errors/error-code->ex-info
                :entity-not-found
                {:entity-type :plan
                 :entity-id plan-id
                 :operation :delete-plan})))
      (try
        {:status 200
         :headers {}
         :body (edges/plan-clj->api (db.plans/delete-plan
                                     database
                                     {:id (:id existing-plan)
                                      :updated-by updated-by}))}
        (catch Exception e
          (throw (errors/error-code->ex-info
                  :entity-deletion-internal-error
                  {:entity-type :plan
                   :entity-id plan-id}
                  e)))))))
