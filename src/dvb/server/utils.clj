(ns dvb.server.utils
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]))

(defn pretty [x] (with-out-str (pprint/pprint x)))

(defn parse-main-opts [opts]
  (if (map? opts)
    opts
    (edn/read-string opts)))
