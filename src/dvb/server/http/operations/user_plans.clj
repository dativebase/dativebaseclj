(ns dvb.server.http.operations.user-plans
  (:require [dvb.common.edges.plans :as plan-edges]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]))

(defn handle [{:as _application :keys [database]}
              {:as ctx {user-id :user_id} :path}]
  (log/info "Getting plans for a user.")
  (authorize/authorize ctx)
  {:status 200
   :headers {}
   :body (mapv plan-edges/clj->api
               (db.plans/get-plans-for-user database user-id))})
