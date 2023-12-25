(ns dvb.common.specs.plans
  "Specs for describing plans. A plan is a map. All plans in DativeBase business
  logic should conform to ``::plan``."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]))

(s/def ::id uuid?)
(s/def ::tier #{:free :subscriber :supporter})
(s/def ::created-at ::common/instant)
(s/def ::inserted-at ::common/instant)
(s/def ::updated-at ::common/instant)
(s/def ::destroyed-at (s/nilable ::common/instant))
(s/def ::created-by (s/nilable uuid?))
(s/def ::updated-by (s/nilable uuid?))

(s/def ::plan
  (s/keys :req-un [::id
                   ::tier
                   ::created-at
                   ::inserted-at
                   ::updated-at
                   ::destroyed-at
                   ::created-by
                   ::updated-by]))

(s/def ::plans (s/coll-of ::plan))

(defn plan? [x] (s/valid? ::plan x))

(defn plans? [x] (s/valid? ::plans x))

(comment

  (plan? (gen/generate (s/gen ::plan)))

  (plans? [(gen/generate (s/gen ::plan))
           (gen/generate (s/gen ::plan))])

)
