(ns old.core
  "A Jetty/Ring HTTP server for serving the OLD REST API and a Swagger UI for
   interacting with it. This supports `lein api`."
  (:require [com.stuartsierra.component :as component]
            [old.http.server :as server]
            [old.http.openapi.spec :as spec]
            #_[old.http.operations.create-form :as create-form]
            #_[old.http.operations.index-forms :as index-forms]
            [old.http.security.api-key :as api-key]))

(defn -main []
  (let [port 8080
        system (component/start
                (component/system-map
                 :application (server/map->Application
                               {:spec spec/api
                                :operations {}
                                :security-handlers {:api-key api-key/handle}})
                 :web-server (component/using (server/map->WebServer {:handler-fn #'server/app
                                                                      :port port}))))]
    (println "Serving OLD HTTP API at http://localhost:8080/.")
    (println "Serving Swagger UI at http://localhost:8080/swagger-ui/dist/index.html.")
    (-> system :web-server :shutdown deref)))
