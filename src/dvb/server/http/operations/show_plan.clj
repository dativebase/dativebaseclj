(ns dvb.server.http.operations.show-plan
  (:require [dvb.common.edges.plans :as plan-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {plan-id :plan_id} :path
    {include-members? :include-members
     include-olds? :include-olds} :query}]
  (log/info "Showing a plan." {:plan-id plan-id
                               :include-members? include-members?
                               :include-olds? include-olds?})
  (authorize/authorize ctx)
  (let [plan
        ((cond
           (and include-olds? include-members?) db.plans/get-plan-with-olds-and-members
           include-members? db.plans/get-plan-with-members
           include-olds? db.plans/get-plan-with-olds
           :else db.plans/get-plan)
         database plan-id)]
    (when-not plan
      (let [data {:entity-type :plan
                  :entity-id plan-id
                  :operation :show-plan}]
        (log/warn "Plan not found" data)
        (throw (errors/error-code->ex-info :entity-not-found data))))
    {:status 200
     :headers {}
     :body (plan-edges/clj->api plan)}))
