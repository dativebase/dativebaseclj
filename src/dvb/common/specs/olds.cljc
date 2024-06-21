(ns dvb.common.specs.olds
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]
            [dvb.common.specs.user-olds :as user-old-specs]))

(s/def ::slug :old/slug)
(s/def ::name ::common/non-empty-string)
(s/def ::created-at ::common/created-at)
(s/def ::inserted-at ::common/inserted-at)
(s/def ::updated-at ::common/updated-at)
(s/def ::destroyed-at ::common/destroyed-at)
(s/def ::created-by uuid?)
(s/def ::updated-by uuid?)

(s/def ::user-old-id ::user-old-specs/id)
(s/def ::user
  (s/keys :req-un [:dvb.common.specs.users/id
                   ::user-old-specs/role
                   ::user-old-id]))
(s/def ::users (s/coll-of ::user))

(s/def ::old
  (s/keys :req-un [::slug
                   ::name
                   ::created-at
                   ::updated-at
                   ::destroyed-at
                   ::created-by
                   ::updated-by]
          :opt-un [::inserted-at
                   ::users]))

(s/def ::olds (s/coll-of ::old))

(s/def ::old-write
  (s/keys :req-un [::slug
                   ::name]))

(s/def ::old-update
  (s/keys :req-un [::name]))

(defn old? [x] (s/valid? ::old x))

(defn old-explain-data [x] (s/explain-data ::old x))

(defn olds? [x] (s/valid? ::olds x))

(defn old-write? [x] (s/valid? ::old-write x))

(defn old-update? [x] (s/valid? ::old-update x))

(comment

  (old? (gen/generate (s/gen ::old)))

  (olds? [(gen/generate (s/gen ::old))
          (gen/generate (s/gen ::old))])

  (old-write? (gen/generate (s/gen ::old-write)))

)
