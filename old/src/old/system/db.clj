(ns old.system.db
  (:require [com.stuartsierra.component :as component]
            [hikari-cp.core :as hikari-cp])
  (:import [java.io Closeable]
           [com.zaxxer.hikari HikariDataSource]))

;; TODO real config from env
(def config
  {:postgresql
   {:old
    {:uri "jdbc:postgresql://localhost:5432/old?user=postgres&password="
     :hikari-cp-opts
     {:socket-timeout "60"}}}})

(defrecord Pool [^HikariDataSource datasource database-name]
  Closeable
  (close [_]
    (when datasource
      (.close datasource))))

(defn- create-datasource-map
  "Create the map for creating the datasource by merging the options from the
   environment with the JDBC URI and the read-only? parameter."
  [jdbc-uri {:keys [hikari-cp-opts]} pool-name]
  (merge
   {:socket-timeout 30}
   ;; Prefer the `jdbc-uri`; these options can override the jdbc-uri. See https://github.com/tomekw/hikari-cp/blob/1.8.3/src/hikari_cp/core.clj#L184-L188 and https://github.com/brettwooldridge/HikariCP/blob/HikariCP-2.7.4/src/main/java/com/zaxxer/hikari/pool/PoolBase.java#L337-L341.
   (dissoc hikari-cp-opts :adapter :datasource-classname :datasource)
   {:jdbc-url jdbc-uri
    :pool-name pool-name
    :read-only false}))

(def datasource (comp hikari-cp/make-datasource create-datasource-map))

(defn psql-connect
  "Create a Pool which can be passed as the first argument to HugSQL functions.
   Expects environment to have environment variable named similar to
   POSTGRESQL__YOUR_DATABASE__URI and a value formatted like
   'jdbc:postgresql://some.host.com:5432/your_database?user=user&password=pass'."
  [database-name]
  (if-let [config (get-in config [:postgresql database-name])]
    (let [pool-name (str (name database-name) "-read-write")
          jdbc-uri (:uri config)
          datasource (datasource jdbc-uri config pool-name)]
      (->Pool datasource database-name))
    (throw (IllegalArgumentException.
            ^String (format "%s is not a valid database name" database-name)))))

(defprotocol IDB
  (conn [this]))

;; TODO: healthcheck

(defrecord DB [db-name db]
  component/Lifecycle
  (start [component]
    (if db
      component
      (assoc component :db (psql-connect db-name))))
  (stop [component]
    (if db
      (do
        (.close db)
        (assoc component :db nil))
      component))
  IDB
  (conn [_]
    db))
