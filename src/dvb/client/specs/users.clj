(ns dvb.client.specs.users
  "Specs for describing users as accepted by the DVB client."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.common :as common]))

(s/def ::first-name ::common/non-empty-string)
(s/def ::last-name ::common/non-empty-string)
(s/def ::email ::common/non-empty-string)
(s/def ::password ::common/non-empty-string)
(s/def ::is-superuser? boolean?)

(s/def ::user-write
  (s/keys :req-un [::first-name
                   ::last-name
                   ::email
                   ::password
                   ::is-superuser?]))

(comment

  (gen/generate (s/gen ::user-write))

)
