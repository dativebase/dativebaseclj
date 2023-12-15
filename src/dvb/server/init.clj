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
            [taoensso.timbre :as log])
  (:import (java.util UUID)))

(defn default-user []
  {:first-name "Anne"
   :last-name "Boleyn"
   :email "ab@qmail.com"
   :password (str (UUID/randomUUID))})

(defn initialize
  "Initialize a fresh DativeBase system with a user."
  ([database] (initialize database {}))
  ([database overrides]
   (let [user (users/create-user database (merge (default-user) overrides))]
     {:user (dissoc user :password)})))

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
