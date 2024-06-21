(ns dvb.common.specs.forms
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]))

(s/def ::id uuid?)
(s/def ::old-slug keyword?)
(s/def ::transcription ::common/non-empty-string)
(s/def ::created-at ::common/created-at)
(s/def ::inserted-at ::common/inserted-at)
(s/def ::updated-at ::common/updated-at)
(s/def ::destroyed-at ::common/destroyed-at)
(s/def ::created-by uuid?)
(s/def ::updated-by uuid?)

(s/def ::form
  (s/keys :req-un [::id
                   ::old-slug
                   ::transcription
                   ::created-at
                   ::updated-at
                   ::destroyed-at
                   ::created-by
                   ::updated-by]
          :opt-un [::inserted-at]))

(s/def ::forms (s/coll-of ::form))

(s/def ::form-write
  (s/keys :req-un [::transcription]))

(s/def ::create-form
  (s/keys :req-un [::old-slug
                   ::transcription
                   ::created-by]))

(defn form? [x] (s/valid? ::form x))

(defn forms? [x] (s/valid? ::forms x))

(defn form-write? [x] (s/valid? ::form-write x))

(comment

  (gen/generate (s/gen ::form))

)


