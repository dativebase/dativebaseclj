(ns dvb.server.http.operations.new-user
  (:require [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]))

(defn handle [_system ctx]
  (log/info "Getting data needed to create a new user.")
  (authorize/authorize ctx)
  {:status 200
   :headers {}
   :body {}})
