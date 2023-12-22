(ns dvb.server.init
  "Tools for initializing a DativeBase server (API)."
  {:clj-kondo/config '{:linters {:unresolved-symbol {:level :off}}}}
  (:require [com.stuartsierra.component :as component]
            dvb.server.db.core
            [dvb.server.db.users :as users]
            [dvb.server.system.config :as config]
            [dvb.server.system.db :as db]
            [dvb.server.system.log :as system.log]
            [dvb.server.utils :as utils]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(def user-defaults
  {:is-superuser? false
   :created-by nil
   :updated-by nil})

(def superuser-password (str (UUID/randomUUID)))

(defn default-superuser []
  (merge user-defaults
         {:first-name "Super"
          :last-name "User"
          :email "su@qmail.com"
          :password superuser-password
          :is-superuser? true}))

(def user-password (str (UUID/randomUUID)))

(defn default-user []
  (merge user-defaults
         {:first-name "Anne"
          :last-name "Boleyn"
          :email "ab@qmail.com"
          :password user-password
          :is-superuser? false}))

(defn initialize
  "Initialize a fresh DativeBase system with a user."
  ([database] (initialize database {}))
  ([database {user-overrides :user superuser-overrides :superuser}]
   (let [user (users/create-user database (merge (default-user) user-overrides))
         superuser (users/create-user database (merge (default-superuser)
                                                      superuser-overrides))]
     {:user (dissoc user :password)
      :superuser (dissoc superuser :password)})))

(defn -main
  "Entrypoint for tools.deps alias `init`, eg `clj -X:init`."
  ([] (-main "{}"))
  ([opts]
   (let [opts (utils/parse-main-opts opts)
         user-overrides (select-keys (->> opts
                                          (map (juxt key (comp name val)))
                                          (into {}))
                                     (keys (default-user)))
         config (config/init (or (:config-path opts) config/dev-config-path))
         db (component/start (db/make-db (:db config)))]
     (system.log/init (:log-file-path config))
     (try
       (log/info "Initialization success." (initialize db user-overrides))
       (catch Exception e
         (log/warn "Failed to initialize." {:error? true
                                            :ex-message (ex-message e)
                                            :ex-data (ex-data e)}))
       (finally (component/stop db))))))
