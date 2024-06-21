(ns dvb.server.http.operations.new-form
  (:require [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]))

(defn handle [_system ctx]
  (log/info "Getting data needed to create a new form.")
  (authorize/authorize ctx)
  {:status 200
   :headers {}
   :body {:grammaticalities ["*" "?"]}})
