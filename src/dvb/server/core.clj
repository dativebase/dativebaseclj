(ns dvb.server.core
  (:require [com.stuartsierra.component :as component]
            dvb.server.db.core
            [dvb.common.openapi.spec :as spec]
            [dvb.server.http.server :as server]
            [dvb.server.http.operations.access-requests-for-single-old :as
             access-requests-for-single-old]
            [dvb.server.http.operations.activate-user :as activate-user]
            [dvb.server.http.operations.approve-old-access-request :as approve-old-access-request]
            [dvb.server.http.operations.create-form :as create-form]
            [dvb.server.http.operations.create-old :as create-old]
            [dvb.server.http.operations.create-old-access-request :as create-old-access-request]
            [dvb.server.http.operations.create-plan :as create-plan]
            [dvb.server.http.operations.create-user :as create-user]
            [dvb.server.http.operations.create-user-old :as create-user-old]
            [dvb.server.http.operations.create-user-plan :as create-user-plan]
            [dvb.server.http.operations.deactivate-user :as deactivate-user]
            [dvb.server.http.operations.delete-form :as delete-form]
            [dvb.server.http.operations.delete-old :as delete-old]
            [dvb.server.http.operations.delete-plan :as delete-plan]
            #_[dvb.server.http.operations.delete-user :as delete-user]
            [dvb.server.http.operations.delete-user-plan :as delete-user-plan]
            [dvb.server.http.operations.edit-form :as edit-form]
            [dvb.server.http.operations.edit-user :as edit-user]
            [dvb.server.http.operations.initiate-password-reset :as
             initiate-password-reset]
            [dvb.server.http.operations.index-forms :as index-forms]
            [dvb.server.http.operations.index-olds :as index-olds]
            [dvb.server.http.operations.index-users :as index-users]
            [dvb.server.http.operations.login :as login]
            [dvb.server.http.operations.new-form :as new-form]
            [dvb.server.http.operations.new-user :as new-user]
            [dvb.server.http.operations.reject-old-access-request :as reject-old-access-request]
            [dvb.server.http.operations.reset-password :as reset-password]
            [dvb.server.http.operations.retract-old-access-request :as retract-old-access-request]
            [dvb.server.http.operations.show-form :as show-form]
            [dvb.server.http.operations.show-old :as show-old]
            [dvb.server.http.operations.show-old-access-request :as show-old-access-request]
            [dvb.server.http.operations.show-plan :as show-plan]
            [dvb.server.http.operations.show-user :as show-user]
            [dvb.server.http.operations.update-form :as update-form]
            [dvb.server.http.operations.update-old :as update-old]
            [dvb.server.http.operations.update-user :as update-user]
            [dvb.server.http.operations.update-user-old :as update-user-old]
            [dvb.server.http.operations.update-user-plan :as update-user-plan]
            [dvb.server.http.operations.user-plans :as user-plans]
            [dvb.server.http.security.api-key :as api-key]
            [dvb.server.system.config :as config]
            [dvb.server.system.clock :as clock]
            [dvb.server.system.db :as db]
            [dvb.server.system.email :as email]
            [dvb.server.system.log :as system.log]
            dvb.server.time
            [dvb.server.utils :as utils]
            [dvb.server.log :as log]))

(def operations
  {:access-requests-for-single-old access-requests-for-single-old/handle
   :activate-user activate-user/handle
   :approve-old-access-request approve-old-access-request/handle
   :create-form create-form/handle
   :create-old create-old/handle
   :create-old-access-request create-old-access-request/handle
   :create-plan create-plan/handle
   :create-user create-user/handle
   :create-user-old create-user-old/handle
   :create-user-plan create-user-plan/handle
   :deactivate-user deactivate-user/handle
   :delete-form delete-form/handle
   :delete-old delete-old/handle
   :delete-plan delete-plan/handle
   #_#_:delete-user delete-user/handle ;; Note: intentionally disabled
   :delete-user-plan delete-user-plan/handle
   :edit-form edit-form/handle
   :edit-user edit-user/handle
   :initiate-password-reset initiate-password-reset/handle
   :index-forms index-forms/handle
   :index-olds index-olds/handle
   :index-users index-users/handle
   :login login/handle
   :new-form new-form/handle
   :new-user new-user/handle
   :reject-old-access-request reject-old-access-request/handle
   :reset-password reset-password/handle
   :retract-old-access-request retract-old-access-request/handle
   :show-form show-form/handle
   :show-old show-old/handle
   :show-old-access-request show-old-access-request/handle
   :show-plan show-plan/handle
   :show-user show-user/handle
   :update-form update-form/handle
   :update-old update-old/handle
   :update-user update-user/handle
   :update-user-old update-user-old/handle
   :update-user-plan update-user-plan/handle
   :user-plans user-plans/handle})

(defn make-main-system [config]
  (component/system-map
   :database (db/make-db (:db config))
   :clock (clock/make-clock)
   :email (email/make-email)
   :application (component/using
                 (server/make-application
                  {:spec spec/api
                   :operations operations
                   :security-handlers {:api-key api-key/handle}})
                 [:clock
                  :database
                  :email])
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
