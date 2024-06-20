(ns dvb.legacy-old-client.utils
  (:require [clojure.string :as str]
            [cheshire.core :as json]))

(defn l-strip
  "Left-strip pfx from s"
  [pfx s]
  (if (str/starts-with? s pfx)
    (l-strip pfx (apply str (drop (count pfx) s)))
    s))

(defn r-strip
  "Right-strip sfx from s"
  [sfx s]
  (if (str/ends-with? s sfx)
    (r-strip sfx (apply str (take (- (count s) (count sfx)) s)))
    s))

(defn json-stringify [thing] (json/generate-string thing {:key-fn name}))

(defn json-parse [thing] (json/parse-string thing true))

