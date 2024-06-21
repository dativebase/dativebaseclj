(ns dvb.server.specs.forms
  (:require [clojure.spec.alpha :as s]
            [dvb.server.specs.common :as common]))

(s/def ::id uuid?)
(s/def ::old-slug string?)
(s/def ::transcription ::common/non-empty-string)
(s/def ::created-by uuid?)

(s/def ::form
  (s/keys :req-un [::id
                   ::old-slug
                   ::transcription
                   ::created-by]))

(s/def ::create-form
  (s/keys :req-un [::old-slug
                   ::transcription
                   ::created-by]))
