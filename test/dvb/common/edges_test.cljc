(ns dvb.common.edges-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [dvb.common.edges :as sut]
            [dvb.common.openapi.serialize :as serialize]
            [dvb.common.openapi.spec :as spec]
            [dvb.common.specs.api-keys :as api-key-specs]
            [dvb.common.specs.forms :as form-specs]
            [dvb.common.specs.plans :as plan-specs]
            [dvb.common.specs.users :as user-specs]
            [dvb.common.openapi.validate :as validate]))


(defspec generated-clj-users-can-always-be-transformed-into-valid-api-users
  (let [user-schema (-> spec/api serialize/denormalize :components :schemas
                        :User)]
    (prop/for-all [user (gen/fmap sut/user-clj->api (s/gen ::user-specs/user))]
                  (= user (validate/validate user user-schema)))))

(defspec generated-clj-plans-can-always-be-transformed-into-valid-api-plans
  (prop/for-all [plan (gen/fmap sut/plan-clj->api (s/gen ::plan-specs/plan))]
                (= plan (validate/validate plan (:Plan sut/schemas)))))

(defspec generated-clj-forms-can-always-be-transformed-into-valid-api-forms
  (prop/for-all [form (gen/fmap sut/form-clj->api (s/gen ::form-specs/form))]
                (= form (validate/validate form (:Form sut/schemas)))))

(defspec generated-clj-api-keys-can-always-be-transformed-into-valid-api-api-keys
  (prop/for-all [api-key (gen/fmap sut/api-key-clj->api (s/gen ::api-key-specs/api-key))]
                (= api-key (validate/validate api-key (:APIKey sut/schemas)))))

(comment

  (def plan (sut/plan-clj->api (gen/generate (s/gen ::plan-specs/plan))))

  plan

  (validate/validate plan (:Plan sut/schemas))

)
