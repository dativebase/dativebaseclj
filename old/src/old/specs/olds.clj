(ns old.specs.olds
  (:require [clojure.spec.alpha :as s]
            [old.specs.common :as common]))

(s/def ::slug ::common/non-empty-string)
(s/def ::name ::common/non-empty-string)

(s/def ::old
  (s/keys :req-un [::slug
                   ::name]))
