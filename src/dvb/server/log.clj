(ns dvb.server.log
  (:require [dvb.server.scrub :as scrub]
            [taoensso.timbre :as log]))

(defmacro info [& args]
  `(log/info ~@(for [a args] (if (coll? a) (scrub/scrub a) a))))

(defmacro warn [& args]
  `(log/warn ~@(for [a args] (if (coll? a) (scrub/scrub a) a))))

(defmacro error [& args]
  `(log/error ~@(for [a args] (if (coll? a) (scrub/scrub a) a))))
