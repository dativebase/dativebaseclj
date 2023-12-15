(ns dvb.server.http.server
  "A Jetty/Ring HTTP server for serving the DativeBase REST API and a Swagger UI
  for interacting with it. This supports `lein api`."
  (:require [com.stuartsierra.component :as component]
            [dvb.server.http.openapi.handle :as openapi.handle]
            [dvb.common.openapi.serialize :as serialize]
            [dvb.server.http.ring-utils :as ring-utils]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :as defaults-mid]
            [ring.middleware.json :as json-mid]
            [ring.middleware.resource :as resource-mid]))

(defn app [app-component]
  (fn [req]
    ((-> (partial openapi.handle/handle app-component)
         ring-utils/wrap-cache-headers
         ring-utils/wrap-declojurify-response
         ring-utils/wrap-clojurify-request
         json-mid/wrap-json-response
         (json-mid/wrap-json-body
          {:bigdecimals? false
           :malformed-response ring-utils/malformed-json-in-request-json-response})
         (defaults-mid/wrap-defaults defaults-mid/api-defaults)
         ;; NOTE: You must install the Swagger UI static resources before there
         ;; is anything to serve under /resources/public/.
         ;; To do this, run `make install-swagger-ui`.
         (resource-mid/wrap-resource "public"))
     req)))

(defrecord Application [spec operations security-handlers database]
  component/Lifecycle
  (start [this] (update this :spec serialize/denormalize))
  (stop [this] this))

(defn make-application [{:keys [spec operations security-handlers]}]
  (map->Application
   {:spec spec
    :operations operations
    :security-handlers security-handlers}))

(defrecord WebServer [handler-fn port application http-server shutdown]
  component/Lifecycle
  (start [this]
    (if http-server
      this
      (assoc this
             :http-server (jetty/run-jetty (handler-fn application)
                                           {:port port
                                            :join? false})
             :shutdown (promise))))
  (stop [this]
    (if http-server
      (do (.stop http-server)
          (deliver shutdown true)
          (assoc this :http-server nil))
      this)))

(defn make-web-server [{:keys [port]}]
  (map->WebServer {:handler-fn app
                   :port port}))
