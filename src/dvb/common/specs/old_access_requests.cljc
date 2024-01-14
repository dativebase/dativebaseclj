(ns dvb.common.specs.old-access-requests
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]
            dvb.common.specs.olds
            [dvb.common.specs.users :as user-specs]))

(s/def ::id uuid?)
(s/def ::old-slug :old/slug)
(s/def ::status #{:pending
                  :approved
                  :rejected
                  :retracted})
(s/def ::user-id ::user-specs/id)
(s/def ::created-at ::common/created-at)

(s/def ::old-access-request
  (s/keys :req-un [::id
                   ::old-slug
                   ::status
                   ::user-id
                   ::created-at]))

(s/def ::old-access-request-write
  (s/keys :req-un [::old-slug
                   ::user-id]))

(s/def ::old-access-requests (s/coll-of ::old-access-request))

(defn old-access-request? [x] (s/valid? ::old-access-request x))

(defn old-access-requests? [x] (s/valid? ::old-access-requests x))

(defn old-access-request-write? [x] (s/valid? ::old-access-request-write x))

(comment

  (old-access-request? (gen/generate (s/gen ::old-access-request)))

  (old-access-requests? [(gen/generate (s/gen ::old-access-request))
                         (gen/generate (s/gen ::old-access-request))])

  (old-access-request-write? (gen/generate (s/gen ::old-access-request-write)))

)
