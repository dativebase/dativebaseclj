(ns dvb.common.utils
  {:clj-kondo/config '{:linters {:unresolved-namespace {:level :off}}}} ;; not sure why kondo doesn't like my env-dependent requires, but this silences warnings ...
  #?(:clj (:refer-clojure :exclude [format]))
  (:require
   [clojure.string :as str]
   #?(:cljs [goog.string :as gstring])
   #?(:cljs [goog.string.format])
   #?(:cljs [cljs-time.format :as cljs-timef])
   #?(:clj [java-time.api :as jt]))
  #?(:clj (:import (java.util UUID))))

(defn normalize-email [email]
  (-> email
      str/trim
      str/lower-case))

(def format
  #?(:clj clojure.core/format
     :cljs gstring/format))

(defn str->uuid [s]
  #?(:clj (UUID/fromString s)
     :cljs (uuid s)))

(defn uuid->str [u]
  #?(:clj (str u)
     :cljs (str u)))

(defn maybe-uuid->str [maybe-u]
  (when maybe-u (str maybe-u)))

(defn maybe-str->uuid [maybe-s]
  (when maybe-s
    (if (uuid? maybe-s)
      maybe-s
      (str->uuid maybe-s))))

(defn str->instant [s]
  #?(:clj (jt/instant s)
     :cljs (try (cljs-timef/parse
                 (cljs-timef/formatters :date-hour-minute-second-ms)
                 (str/replace s #"\.(\d{3})\d{3}Z$" ".$1"))
                (catch js/Error e
                  (println "Failed to parse string instant in JavaScript land.")
                  (println e)
                  s))))

(defn instant->str [i]
  #?(:clj (str i)
     :cljs i))

(defn maybe-str->instant [maybe-s]
  #?(:clj (when maybe-s (jt/instant maybe-s))
     :cljs maybe-s))

(defn maybe-instant->str [maybe-i]
  (when maybe-i (str maybe-i)))

(defn name-keyword-or-identity [thing]
  (if (keyword? thing) (name thing) thing))

(defn parse-int [digits]
  #?(:clj (Integer/parseInt digits)
     :cljs (js/parseInt digits 10)))
