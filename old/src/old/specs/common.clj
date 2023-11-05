(ns old.specs.common
  (:require [clojure.spec.alpha :as s]))

(s/def ::non-empty-string (s/and string? (complement empty?)))

