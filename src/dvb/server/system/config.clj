(ns dvb.server.system.config
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as s])
  (:import (java.io File)))

(s/def ::server-port
  (s/int-in 1024 (inc 65535)))
(s/def ::log-file-path string?)
(s/def :db/name string?)
(s/def :db/user string?)
(s/def :db/password string?)
(s/def ::db
  (s/keys :req-un [:db/name
                   :db/user
                   :db/password]))
(s/def ::config
  (s/keys :req-un [::server-port
                   ::log-file-path
                   ::db]))

(defn read-config-file [config-path]
  (try
    (-> config-path slurp edn/read-string)
    (catch Exception e
      (throw (ex-info "Malformed config file" {:config-path config-path
                                               :ex-message (ex-message e)
                                               :ex-data (ex-data e)})))))

(defn coerce-config [config]
  (when-not (s/valid? ::config config)
    (throw (ex-info "Config is invalid"
                    {:config config
                     :explain-data (s/explain-data ::config config)})))
  config)

(defn validate-config-path
  [config-path]
  (when-not (.exists (new File config-path))
    (throw (ex-info "The config file path does not exist."
                    {:config-path config-path})))
  config-path)

(def dev-config-path "dev-config.edn")

(defn init [config-path]
  (-> config-path
      validate-config-path
      read-config-file
      coerce-config))

(comment

  (init dev-config-path)

)
