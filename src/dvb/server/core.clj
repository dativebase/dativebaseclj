(ns dvb.server.core
  (:require [com.stuartsierra.component :as component]
            dvb.server.db.core
            [dvb.common.openapi.spec :as spec]
            [dvb.server.http.server :as server]
            [dvb.server.http.operations.activate-user :as activate-user]
            [dvb.server.http.operations.create-form :as create-form]
            [dvb.server.http.operations.create-plan :as create-plan]
            [dvb.server.http.operations.create-user :as create-user]
            [dvb.server.http.operations.create-user-plan :as create-user-plan]
            [dvb.server.http.operations.delete-form :as delete-form]
            [dvb.server.http.operations.delete-plan :as delete-plan]
            [dvb.server.http.operations.delete-user :as delete-user]
            [dvb.server.http.operations.edit-form :as edit-form]
            [dvb.server.http.operations.edit-user :as edit-user]
            [dvb.server.http.operations.index-forms :as index-forms]
            [dvb.server.http.operations.index-users :as index-users]
            [dvb.server.http.operations.login :as login]
            [dvb.server.http.operations.new-form :as new-form]
            [dvb.server.http.operations.new-user :as new-user]
            [dvb.server.http.operations.show-form :as show-form]
            [dvb.server.http.operations.show-plan :as show-plan]
            [dvb.server.http.operations.show-user :as show-user]
            [dvb.server.http.operations.update-form :as update-form]
            [dvb.server.http.operations.update-user :as update-user]
            [dvb.server.http.operations.user-plans :as user-plans]
            [dvb.server.http.security.api-key :as api-key]
            [dvb.server.system.config :as config]
            [dvb.server.system.clock :as clock]
            [dvb.server.system.db :as db]
            [dvb.server.system.log :as system.log]
            dvb.server.time
            [dvb.server.utils :as utils]
            [dvb.server.log :as log]))

(def operations
  {:activate-user activate-user/handle
   :create-form create-form/handle
   :create-plan create-plan/handle
   :create-user create-user/handle
   :create-user-plan create-user-plan/handle
   :delete-form delete-form/handle
   :delete-plan delete-plan/handle
   :delete-user delete-user/handle
   :edit-form edit-form/handle
   :edit-user edit-user/handle
   :index-forms index-forms/handle
   :index-users index-users/handle
   :login login/handle
   :new-form new-form/handle
   :new-user new-user/handle
   :show-form show-form/handle
   :show-plan show-plan/handle
   :show-user show-user/handle
   :update-form update-form/handle
   :update-user update-user/handle
   :user-plans user-plans/handle})

(defn make-main-system [config]
  (component/system-map
   :database (db/make-db (:db config))
   :clock (clock/make-clock)
   :application (component/using
                 (server/make-application
                  {:spec spec/api
                   :operations operations
                   :security-handlers {:api-key api-key/handle}})
                 [:clock
                  :database])
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
