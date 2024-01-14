(ns dvb.server.http.operations.access-requests-for-single-old
  (:require [dvb.common.edges.old-access-requests :as old-access-request-edges]
            [dvb.server.db.old-access-requests :as db.old-access-requests]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log]))

(defn handle [{:as _application :keys [database]} ctx]
  (let [old-slug (-> ctx :path :old_slug)
        {:as _authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        log-ctx {:old-slug old-slug
                 :authenticated-user-id authenticated-user-id}]
    (log/info "Fetching access requests for a target OLD." log-ctx)
    (authorize/authorize ctx)
    (let [requests
          (db.old-access-requests/get-pending-old-access-requests-for-old
           database old-slug)]
      {:status 200
       :headers {}
       :body (mapv old-access-request-edges/clj->api requests)})))
