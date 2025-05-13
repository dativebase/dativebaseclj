(ns dvb.server.log
  (:require [dvb.server.scrub :as scrub]
            [taoensso.telemere :as tel]))

(defmacro info [& args]
  `(tel/log! {:level :info
              :msg   ~(vec
                       (for [a args]
                         (if (coll? a)
                           (scrub/scrub a)
                           a)))}))

(defmacro warn [& args]
  `(tel/log! {:level :warn
              :msg   ~(vec
                       (for [a args]
                         (if (coll? a)
                           (scrub/scrub a)
                           a)))}))

(defmacro error [& args]
  `(tel/log! {:level :error
              :msg   ~(vec
                       (for [a args]
                         (if (coll? a)
                           (scrub/scrub a)
                           a)))}))
