(ns old.core
  (:require [com.stuartsierra.component :as component]
            old.db.core
            [old.http.server :as server]
            [old.http.openapi.spec :as spec]
            [old.http.operations.create-form :as create-form]
            [old.http.operations.index-forms :as index-forms]
            [old.http.security.api-key :as api-key]
            [old.system.db :as db]
            [old.system.log :as system.log]
            [taoensso.timbre :as log]))

(defn make-main-system [http-port]
  (component/system-map
   :database (db/map->DB {:db-name :old})
   :application (component/using
                 (server/map->Application
                  {:spec spec/api
                   :operations {:index-forms index-forms/handle
                                :create-form create-form/handle}
                   :security-handlers {:api-key api-key/handle}})
                 [:database])
   :web-server (component/using
                (server/map->WebServer {:handler-fn #'server/app
                                        :port http-port})
                [:application])))

(defn -main []
  (let [port 8080
        system (component/start (make-main-system port))]
    (system.log/init)
    (log/info (format "Serving OLD HTTP API at http://localhost:%s/." port))
    (log/info (format "Serving Swagger UI at http://localhost:%s/swagger-ui/dist/index.html." port))
    (-> system :web-server :shutdown deref)))
