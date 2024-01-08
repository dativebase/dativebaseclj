(ns dvb.common.specs.plans
  "Specs for describing plans. A plan is a map. All plans in DativeBase business
  logic should conform to ``::plan``."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]
            [dvb.common.specs.user-plans :as user-plan-specs]))

(s/def ::id uuid?)
(s/def ::tier #{:free :subscriber :supporter})
(s/def ::created-at ::common/created-at)
(s/def ::updated-at ::common/updated-at)
(s/def ::destroyed-at ::common/destroyed-at)
(s/def ::created-by uuid?)
(s/def ::updated-by uuid?)

(s/def ::user-plan-id ::user-plan-specs/id)
(s/def ::member
  (s/keys :req-un [::id
                   ::user-plan-specs/role
                   ::user-plan-id]))

(s/def ::members (s/coll-of ::member))
(s/def ::olds (s/coll-of :old/slug))

(s/def ::plan
  (s/keys :req-un [::id
                   ::tier
                   ::created-at
                   ::updated-at
                   ::destroyed-at
                   ::created-by
                   ::updated-by]
          :opt-un [::members
                   ::olds]))

(s/def ::plans (s/coll-of ::plan))

(s/def ::plan-write
  (s/keys :req-un [::tier]))

(defn plan? [x] (s/valid? ::plan x))

(defn plans? [x] (s/valid? ::plans x))

(defn plan-write? [x] (s/valid? ::plan-write x))

(comment

  (plan? (gen/generate (s/gen ::plan)))

  (plans? [(gen/generate (s/gen ::plan))
           (gen/generate (s/gen ::plan))])

  (plan-write? (gen/generate (s/gen ::plan-write)))

)
