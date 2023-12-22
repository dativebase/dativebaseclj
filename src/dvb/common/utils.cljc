(ns dvb.common.utils
  #?(:clj (:refer-clojure :exclude [format]))
  (:require
   #?(:cljs [goog.string :as gstring])
   #?(:cljs [goog.string.format])
   #?(:clj [java-time.api :as jt]))
  #?(:clj (:import (java.util UUID))))

(def format
  #?(:clj clojure.core/format
     :cljs gstring/format))

(defn ->uuid [s] (UUID/fromString s))

(defn ->instant [s] (jt/instant s))

(defn ->nilable-instant [maybe-s]
  (when maybe-s (jt/instant maybe-s)))
