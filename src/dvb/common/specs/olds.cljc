(ns dvb.common.specs.olds
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]))

(s/def ::slug :old/slug)
(s/def ::name ::common/non-empty-string)
(s/def ::created-at ::common/created-at)
(s/def ::inserted-at ::common/inserted-at)
(s/def ::updated-at ::common/updated-at)
(s/def ::destroyed-at ::common/destroyed-at)
(s/def ::created-by uuid?)
(s/def ::updated-by uuid?)

(s/def ::old
  (s/keys :req-un [::slug
                   ::name
                   ::created-at
                   ::updated-at
                   ::destroyed-at
                   ::created-by
                   ::updated-by]
          :opt-un [::inserted-at]))

(s/def ::olds (s/coll-of ::old))

(s/def ::old-write
  (s/keys :req-un [::slug
                   ::name]))

(defn old? [x] (s/valid? ::old x))

(defn olds? [x] (s/valid? ::olds x))

(defn old-write? [x] (s/valid? ::old-write x))

(comment

  (old? (gen/generate (s/gen ::old)))

  (olds? [(gen/generate (s/gen ::old))
          (gen/generate (s/gen ::old))])

  (old-write? (gen/generate (s/gen ::old-write)))

)
