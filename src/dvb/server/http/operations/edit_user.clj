(ns dvb.server.http.operations.edit-user
  (:require [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]))

(defn handle [_system ctx]
  (log/info "Getting data needed to update an existing user.")
  (authorize/authorize ctx)
  {:status 200
   :headers {}
   :body {}})
