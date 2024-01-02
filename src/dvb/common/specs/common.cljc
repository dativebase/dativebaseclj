(ns dvb.common.specs.common
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str]
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

(s/def :old/slug
  (s/with-gen
    ::non-empty-string
    #(gen/fmap (fn [slug] (str/replace slug "-" ""))
               (s/gen uuid?))))

(defn uuid-string? [x]
  (and (string? x)
       (re-find #"^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$" x)))

(s/def ::uuid-string
  (s/with-gen
    uuid-string?
    #(gen/fmap str (s/gen uuid?))))

(s/def ::created-at ::instant)
(s/def ::inserted-at ::instant)
(s/def ::updated-at ::instant)
(s/def ::destroyed-at (s/nilable ::instant))
(s/def ::created-by (s/nilable uuid?))
(s/def ::updated-by (s/nilable uuid?))
