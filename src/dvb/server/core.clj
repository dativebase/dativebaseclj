(ns dvb.server.core
  (:require [com.stuartsierra.component :as component]
            dvb.server.db.core
            [dvb.common.openapi.spec :as spec]
            [dvb.server.http.server :as server]
            [dvb.server.http.operations.create-form :as create-form]
            [dvb.server.http.operations.delete-form :as delete-form]
            [dvb.server.http.operations.index-forms :as index-forms]
            [dvb.server.http.operations.login :as login]
            [dvb.server.http.operations.show-form :as show-form]
            [dvb.server.http.operations.show-user :as show-user]
            [dvb.server.http.operations.update-form :as update-form]
            [dvb.server.http.security.api-key :as api-key]
            [dvb.server.system.config :as config]
            [dvb.server.system.db :as db]
            [dvb.server.system.log :as system.log]
            dvb.server.time
            [dvb.server.utils :as utils]
            [taoensso.timbre :as log]))

(def operations
  {:index-forms index-forms/handle
   :create-form create-form/handle
   :delete-form delete-form/handle
   :show-form show-form/handle
   :show-user show-user/handle
   :update-form update-form/handle
   :login login/handle})

(defn make-main-system [config]
  (component/system-map
   :database (db/make-db (:db config))
   :application (component/using
                 (server/make-application
                  {:spec spec/api
                   :operations operations
                   :security-handlers {:api-key api-key/handle}})
                 [:database])
   :web-server (component/using
                (server/make-web-server {:port (:server-port config)})
                [:application])))

(defn -main [opts]
  (let [opts (utils/parse-main-opts opts)
        {:as config :keys [server-port log-file-path]}
        (config/init (or (:config-path opts) config/dev-config-path))]
    (system.log/init log-file-path)
    (let [system (component/start (make-main-system config))]
      (log/info (format "Serving DativeBase HTTP API at http://localhost:%s/."
                        server-port))
      (log/info (format "Serving DativeBase Swagger UI at http://localhost:%s/swagger-ui/dist/index.html."
                        server-port))
      (-> system :web-server :shutdown deref))))
