(ns dvb.common.specs.users
  "Specs for describing users. A user is a map. All users in DativeBase business
  logic should conform to ``::user``."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.plans :as plan-specs]
            [dvb.common.specs.user-plans :as user-plan-specs]
            [dvb.common.specs.common :as common]))

(s/def ::id uuid?)
(s/def ::first-name ::common/non-empty-string)
(s/def ::last-name ::common/non-empty-string)
(s/def ::email ::common/non-empty-string)
(s/def ::password ::common/non-empty-string)
(s/def ::is-superuser? boolean?)
(s/def ::created-at ::common/created-at)
(s/def ::inserted-at ::common/inserted-at)
(s/def ::updated-at ::common/updated-at)
(s/def ::destroyed-at ::common/destroyed-at)
(s/def ::created-by (s/nilable uuid?))
(s/def ::updated-by (s/nilable uuid?))

;; for user-with-plans
(s/def ::plan
  (s/keys :req-un [::id
                   ::plan-specs/tier
                   ::user-plan-specs/role]))

(s/def ::plans (s/coll-of ::plan))

(s/def ::user
  (s/keys :req-un [::id
                   ::first-name
                   ::last-name
                   ::email
                   ::is-superuser?
                   ::created-at
                   ::updated-at
                   ::destroyed-at
                   ::created-by
                   ::updated-by]
          :opt-un [::plans
                   ::inserted-at
                   ::password]))

(s/def ::users (s/coll-of ::user))

(s/def ::user-write
  (s/keys :req-un [::first-name
                   ::last-name
                   ::email
                   ::password
                   ::is-superuser?]))

(defn user? [x] (s/valid? ::user x))

(defn users? [x] (s/valid? ::users x))

(defn user-write? [x] (s/valid? ::user-write x))

(comment

  (user? (gen/generate (s/gen ::user)))

  (users? (gen/generate (s/gen ::users)))

  (user-write? (gen/generate (s/gen ::user-write)))

)
