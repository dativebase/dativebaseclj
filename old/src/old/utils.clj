(ns old.utils
  (:require [clojure.pprint :as pprint]))

(defn pretty [x] (with-out-str (pprint/pprint x)))
