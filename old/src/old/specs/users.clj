(ns old.specs.users
  (:require [clojure.spec.alpha :as s]
            [old.specs.common :as common]))

(s/def ::id uuid?)
(s/def ::first-name ::common/non-empty-string)
(s/def ::last-name ::common/non-empty-string)
(s/def ::email ::common/non-empty-string)
(s/def ::username ::common/non-empty-string)
(s/def ::password ::common/non-empty-string)

(s/def ::user
  (s/keys :req-un [::id
                   ::first-name
                   ::last-name
                   ::email
                   ::username
                   ::password]))

(s/def ::create-user
  (s/keys :req-un [::first-name
                   ::last-name
                   ::email
                   ::username
                   ::password]))
