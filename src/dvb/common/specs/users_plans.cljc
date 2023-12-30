(ns dvb.common.specs.users-plans
  "Specs for describing user plans. A user plan is a map. All user plans in
  DativeBase business logic should conform to ``::user-plan``."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]))

(s/def ::id uuid?)
(s/def ::user-id uuid?)
(s/def ::plan-id uuid?)
(s/def ::role #{:manager :member})
(s/def ::created-at ::common/created-at)
(s/def ::inserted-at ::common/inserted-at)
(s/def ::updated-at ::common/updated-at)
(s/def ::destroyed-at ::common/destroyed-at)
(s/def ::created-by (s/nilable uuid?))
(s/def ::updated-by (s/nilable uuid?))

(s/def ::user-plan
  (s/keys :req-un [::id
                   ::user-id
                   ::plan-id
                   ::role
                   ::created-at
                   ::inserted-at
                   ::updated-at
                   ::destroyed-at
                   ::created-by
                   ::updated-by]))

(s/def ::user-plans (s/coll-of ::user-plan))

(defn user-plan? [x] (s/valid? ::user-plan x))

(defn user-plans? [x] (s/valid? ::user-plans x))

(comment

  (user-plan? (gen/generate (s/gen ::user-plan)))

  (user-plans? (gen/generate (s/gen ::user-plans)))

)
