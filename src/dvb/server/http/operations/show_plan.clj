(ns dvb.server.http.operations.show-plan
  (:require [dvb.common.edges :as edges]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {plan-id :plan_id} :path}]
  (log/info "Showing a plan." {:plan-id plan-id})
  (authorize/authorize ctx)
  (if-let [plan (db.plans/get-plan database plan-id)]
    {:status 200
     :headers {}
     :body (edges/plan-clj->api plan)}
    {:status 404
     :headers {}
     :body
     {:errors
      [{:message "The referenced plan could not be found. Please ensure that the supplied identifier is correct."
        :error-code "entity-not-found"}]}}))
