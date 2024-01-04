(ns dvb.server.http.operations.show-plan
  (:require [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {plan-id :plan_id} :path
    {include-members? :include-members} :query}]
  (log/info "Showing a plan." {:plan-id plan-id
                               :include-members? include-members?})
  (authorize/authorize ctx)
  (let [plan (if include-members?
               (db.plans/get-plan-with-members database plan-id)
               (db.plans/get-plan database plan-id))]
    (when-not plan
      (let [data {:entity-type :plan
                  :entity-id plan-id
                  :operation :show-plan}]
        (log/warn "Plan not found" data)
        (throw (errors/error-code->ex-info :entity-not-found data))))
    {:status 200
     :headers {}
     :body (edges/plan-clj->api plan)}))
