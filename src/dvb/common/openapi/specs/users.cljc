(ns dvb.common.openapi.specs.users
  "Specs for describing users as exposed by the OpenAPI spec. These specs should
  be equivalent to the OpenAPI user-related schemata defined in
  dvb.common.openapi.spec.components.user."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]))

(s/def ::id ::common/uuid-string)
(s/def ::first-name ::common/non-empty-string)
(s/def ::last-name ::common/non-empty-string)
(s/def ::email ::common/non-empty-string)
(s/def ::password ::common/non-empty-string)
(s/def ::is-superuser boolean?)
(s/def ::created-at ::common/instant)
(s/def ::inserted-at ::common/instant)
(s/def ::updated-at ::common/instant)
(s/def ::destroyed-at (s/nilable ::common/instant))
(s/def ::created-by (s/nilable uuid?))
(s/def ::updated-by (s/nilable uuid?))

(s/def ::user-write
  (s/keys :req-un [::first-name
                   ::last-name
                   ::email
                   ::is-superuser?
                   ::password]))

(comment

  (gen/generate (s/gen ::user))

)
