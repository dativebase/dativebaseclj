(ns dvb.common.utils
  {:clj-kondo/config '{:linters {:unresolved-namespace {:level :off}}}} ;; not sure why kondo doesn't like my env-dependent requires, but this silences warnings ...
  #?(:clj (:refer-clojure :exclude [format]))
  (:require
   #?(:cljs [goog.string :as gstring])
   #?(:cljs [goog.string.format])
   #?(:clj [java-time.api :as jt]))
  #?(:clj (:import (java.util UUID))))

(def format
  #?(:clj clojure.core/format
     :cljs gstring/format))

(defn str->uuid [s] (UUID/fromString s))

(defn uuid->str [u] (str u))

(defn maybe-uuid->str [maybe-u]
  (when maybe-u (str maybe-u)))

(defn maybe-str->uuid [maybe-s]
  (when maybe-s
    (if (uuid? maybe-s)
      maybe-s
      (str->uuid maybe-s))))

(defn str->instant [s] (jt/instant s))

(defn instant->str [i] (str i))

(defn maybe-str->instant [maybe-s]
  (when maybe-s (jt/instant maybe-s)))

(defn maybe-instant->str [maybe-i]
  (when maybe-i (str maybe-i)))

(defn name-keyword-or-identity [thing]
  (if (keyword? thing) (name thing) thing))
