(ns old.specs.forms
  (:require [clojure.spec.alpha :as s]
            [old.specs.common :as common]))

(s/def ::id uuid?)
(s/def ::old-slug string?)
(s/def ::transcription ::common/non-empty-string)
(s/def ::created-by-user-id uuid?)

(s/def ::form
  (s/keys :req-un [::id
                   ::old-slug
                   ::transcription
                   ::created-by-user-id]))

(s/def ::create-form
  (s/keys :req-un [::old-slug
                   ::transcription
                   ::created-by-user-id]))
