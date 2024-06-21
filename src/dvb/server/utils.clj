(ns dvb.server.utils
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            dvb.server.time)
  (:refer-clojure :exclude [read-string]))

(defn pretty [x] (with-out-str (pprint/pprint x)))

(defn read-string [s]
  (edn/read-string {:readers *data-readers*} s))

(defn parse-main-opts [opts]
  (if (map? opts)
    opts
    (read-string opts)))
