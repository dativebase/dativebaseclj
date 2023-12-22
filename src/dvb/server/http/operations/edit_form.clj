(ns dvb.server.http.operations.edit-form
  (:require [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]))

(defn handle [_system ctx]
  (log/info "Getting data needed to update an existing form.")
  (authorize/authorize ctx)
  {:status 200
   :headers {}
   :body {:grammaticalities ["*" "?"]}})
