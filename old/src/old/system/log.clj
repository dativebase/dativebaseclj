(ns old.system.log
  (:require [taoensso.timbre :as timbre]))

(defn init []
  (timbre/merge-config!
   {:min-level :info}))
