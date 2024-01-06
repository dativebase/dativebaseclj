(ns dvb.common.specs.user-olds
  "Specs for describing user OLDs. A user OLD is a map. All user OLDs in
  DativeBase business logic should conform to ``::user-old``."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]))

(s/def ::id uuid?)
(s/def ::user-id uuid?)
(s/def ::old-slug :old/slug)
(s/def ::role #{:administrator :contributor :viewer})
(s/def ::created-at ::common/created-at)
(s/def ::updated-at ::common/updated-at)
(s/def ::destroyed-at ::common/destroyed-at)
(s/def ::created-by uuid?)
(s/def ::updated-by uuid?)

(s/def ::user-old
  (s/keys :req-un [::id
                   ::user-id
                   ::old-slug
                   ::role
                   ::created-at
                   ::updated-at
                   ::destroyed-at
                   ::created-by
                   ::updated-by]))

(s/def ::user-olds (s/coll-of ::user-old))

(s/def ::user-old-write
  (s/keys :req-un [::user-id
                   ::old-slug
                   ::role]))

(defn user-old? [x] (s/valid? ::user-old x))

(defn user-olds? [x] (s/valid? ::user-olds x))

(defn user-old-write? [x] (s/valid? ::user-old-write x))

(comment

  (user-old? (gen/generate (s/gen ::user-old)))

  (user-olds? (gen/generate (s/gen ::user-olds)))

  (user-old-write? (gen/generate (s/gen ::user-old-write)))

)
