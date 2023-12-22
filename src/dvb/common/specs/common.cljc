(ns dvb.common.specs.common
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            #?(:clj [java-time.api :as jt])))

(defn random-recent-instant []
  (let [now (jt/instant)
        epoch (jt/instant 0)
        oldest-millis-ago (* 2 365 24 60 60 1000) ;; 2y in millis
        millis-ago (long (rand oldest-millis-ago))
        epoch-to-now-duration-millis (jt/as (jt/duration epoch now) :millis)
        millis-from-epoch (- epoch-to-now-duration-millis millis-ago)]
    (jt/instant millis-from-epoch)))

(s/def ::instant
  (s/with-gen
    jt/instant?
    #(gen/fmap (fn [_] (random-recent-instant))
               (s/gen int?))))

(s/def ::non-empty-string (s/and string? (complement empty?)))

(defn uuid-string? [x]
  (and (string? x)
       (re-find #"^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$" x)))

(s/def ::uuid-string
  (s/with-gen
    uuid-string?
    #(gen/fmap str (s/gen uuid?))))
