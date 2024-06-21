(ns dvb.common.specs.user-plans
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
(s/def ::updated-at ::common/updated-at)
(s/def ::destroyed-at ::common/destroyed-at)
(s/def ::created-by uuid?)
(s/def ::updated-by uuid?)

(s/def ::user-plan
  (s/keys :req-un [::id
                   ::user-id
                   ::plan-id
                   ::role
                   ::created-at
                   ::updated-at
                   ::destroyed-at
                   ::created-by
                   ::updated-by]))

(s/def ::user-plans (s/coll-of ::user-plan))

(s/def ::user-plan-write
  (s/keys :req-un [::user-id
                   ::plan-id
                   ::role]))

(defn user-plan? [x] (s/valid? ::user-plan x))

(defn user-plans? [x] (s/valid? ::user-plans x))

(defn user-plan-write? [x] (s/valid? ::user-plan-write x))

(comment

  (user-plan? (gen/generate (s/gen ::user-plan)))

  (user-plans? (gen/generate (s/gen ::user-plans)))

)
