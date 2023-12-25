(ns dvb.common.specs.forms
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]))

(s/def ::id uuid?)
(s/def ::old-slug string?)
(s/def ::transcription ::common/non-empty-string)
(s/def ::created-at ::common/instant)
(s/def ::inserted-at ::common/instant)
(s/def ::updated-at ::common/instant)
(s/def ::destroyed-at (s/nilable ::common/instant))
(s/def ::created-by uuid?)
(s/def ::updated-by uuid?)

(s/def ::form
  (s/keys :req-un [::id
                   ::old-slug
                   ::transcription
                   ::created-at
                   ::inserted-at
                   ::updated-at
                   ::destroyed-at
                   ::created-by
                   ::updated-by]))

(s/def ::create-form
  (s/keys :req-un [::old-slug
                   ::transcription
                   ::created-by]))

(comment

  (gen/generate (s/gen ::form))

)


