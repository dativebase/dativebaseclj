(ns dvb.server.http.operations.show-old
  (:require [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as u]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {old-slug :old_slug} :path}]
  (log/info "Showing an OLD." {:old-slug old-slug})
  (authorize/authorize ctx)
  (let [old (db.olds/get-old database old-slug)]
    (when-not old
      (let [data {:entity-type :old
                  :old-slug old-slug
                  :operation :show-old}]
        (log/warn "OLD not found" data)
        (throw (errors/error-code->ex-info :entity-not-found data))))
    {:status 200
     :headers {}
     :body (edges/old-clj->api old)}))
