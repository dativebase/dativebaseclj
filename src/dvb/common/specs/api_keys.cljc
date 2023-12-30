(ns dvb.common.specs.api-keys
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]))

(s/def ::id uuid?)
(s/def ::user-id uuid?)
(s/def ::key ::common/non-empty-string)
(s/def ::created-at ::common/created-at)
(s/def ::expires-at ::common/instant)

(s/def ::api-key
  (s/keys :req-un [::id
                   ::user-id
                   ::key
                   ::created-at
                   ::expires-at]))

(comment

  (gen/generate (s/gen ::api-key))

)
