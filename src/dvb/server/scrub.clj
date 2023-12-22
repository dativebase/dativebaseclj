(ns dvb.server.scrub
  (:require [clojure.string :as str]
            [clojure.walk :as walk])
  (:import (clojure.lang MapEntry)))

(def redacted #{:first-name
                :last-name
                :name
                :email
                :email-address
                :password
                :api-key
                :phone
                :phone-number
                :address
                :postal-code
                :affiliation})

(defn nameable? [x]
  (or (keyword? x)
      (string? x)
      (symbol? x)))

(defn homogenize [nameable]
  (-> nameable
      name
      (str/replace "-" "")
      (str/replace "_" "")
      str/lower-case))

(defn redact? [key*]
  (boolean
   (and (nameable? key*)
        (some (fn [redacted-kw]
                (str/includes?
                 (homogenize key*)
                 (homogenize redacted-kw)))
              redacted))))

(defn scrub-node [node]
  (if (map-entry? node)
    (if (redact? (key node))
      (MapEntry. (key node) "*Redacted*")
      node)
    node))

(defn scrub [data]
  (walk/postwalk scrub-node data))

(comment

  (scrub {:a 2
          :b 3
          :password "secret"
          :c {:name "Joel"
              :emails ["ab@c.com" "ZZ@d.com"]}
          :address {:id "abc"
                    :postal-code "aaaa"}})

)
