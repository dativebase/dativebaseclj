(ns dvb.server.system.db
  (:require [com.stuartsierra.component :as component]
            dvb.server.db.core ;; so that the HugSQL customization gets loaded
            [hikari-cp.core :as hikari-cp])
  (:import [java.io Closeable]
           [com.zaxxer.hikari HikariDataSource]))

(defrecord Pool [^HikariDataSource datasource db-config]
  Closeable
  (close [_]
    (when datasource
      (.close datasource))))

(defn psql-connect
  "Create a Pool which can be passed as the first argument to HugSQL functions."
  [{:as db-config :keys [name user password]}]
  (->Pool
   (hikari-cp/make-datasource
    {:socket-timeout 30
     :read-only false
     :jdbc-url (format "jdbc:postgresql://localhost:5432/%s?user=%s&password=%s"
                       name user password)
     :pool-name (str name "-pool")})
   db-config))

(defn start [component]
  (if (:datasource component)
    component
    (psql-connect (:db-config component))))

(defn stop [component]
  (if (:datasource component)
    (-> component
        (doto .close)
        (dissoc :datasource))
    component))

(extend Pool
  component/Lifecycle
  {:start start :stop stop})

(defn make-db [db-config] (map->Pool {:db-config db-config}))
