(ns dvb.server.http.operations.show-old-access-request
  (:require [dvb.common.edges.old-access-requests :as old-access-request-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.old-access-requests :as db.old-access-requests]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {old-access-request-id :old_access_request_id} :path}]
  (let [old-access-request-id (UUID/fromString old-access-request-id)
        old-access-request (db.old-access-requests/get-old-access-request
                            database old-access-request-id)]
    (log/info "Showing an OLD access request."
              {:old-access-request-id old-access-request-id})
    (authorize/authorize ctx)
    (when-not old-access-request
      (let [data {:entity-type :old-access-request
                  :entity-id old-access-request-id
                  :operation :show-old-access-request}]
        (log/warn "OLD access request not found" data)
        (throw (errors/error-code->ex-info :entity-not-found data))))
    {:status 200
     :headers {}
     :body (old-access-request-edges/clj->api old-access-request)}))
