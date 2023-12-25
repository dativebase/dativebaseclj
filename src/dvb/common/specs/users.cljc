(ns dvb.common.specs.users
  "Specs for describing users. A user is a map. All users in DativeBase business
  logic should conform to ``::user``."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]))

(s/def ::id uuid?)
(s/def ::first-name ::common/non-empty-string)
(s/def ::last-name ::common/non-empty-string)
(s/def ::email ::common/non-empty-string)
(s/def ::password ::common/non-empty-string)
(s/def ::is-superuser? boolean?)
(s/def ::created-at ::common/instant)
(s/def ::inserted-at ::common/instant)
(s/def ::updated-at ::common/instant)
(s/def ::destroyed-at (s/nilable ::common/instant))
(s/def ::created-by (s/nilable uuid?))
(s/def ::updated-by (s/nilable uuid?))

(s/def ::user
  (s/keys :req-un [::id
                   ::first-name
                   ::last-name
                   ::email
                   ::password
                   ::is-superuser?
                   ::created-at
                   ::inserted-at
                   ::updated-at
                   ::destroyed-at
                   ::created-by
                   ::updated-by]))

(s/def ::users (s/coll-of ::user))

(defn user? [x] (s/valid? ::user x))

(defn users? [x] (s/valid? ::users x))

(comment

  (user? (gen/generate (s/gen ::user)))

  (users? (gen/generate (s/gen ::users)))

)
