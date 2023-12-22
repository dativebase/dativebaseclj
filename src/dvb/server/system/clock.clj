(ns dvb.server.system.clock
  (:require [com.stuartsierra.component :as component]
            [java-time.api :as jt]))

(defprotocol IClock
  "For, you know, time"
  (now [this] "Get now: the current timestamp"))

(defrecord Clock [now-fn]
  IClock
  (now [_] (now-fn))
  component/Lifecycle
  (start [c] c)
  (stop [c] c))

(defn make-clock []
  (map->Clock {:now-fn jt/instant}))

(comment

  (def c (make-clock))

  (now c)

)
