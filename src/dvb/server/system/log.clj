(ns dvb.server.system.log
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]))

(defn init [log-file-path]
  (timbre/merge-config!
   {:min-level :info
    :appenders {:spit (appenders/spit-appender {:fname log-file-path})}}))
