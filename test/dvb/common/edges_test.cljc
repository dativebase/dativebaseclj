(ns dvb.common.edges-test
  {:clj-kondo/config '{:lint-as {clojure.test.check.clojure-test/defspec clj-kondo.lint-as/def-catch-all}}}
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [dvb.common.edges.api-keys :as api-key-edges]
            [dvb.common.edges.common :as common-edges]
            [dvb.common.edges.forms :as form-edges]
            [dvb.common.edges.plans :as plan-edges]
            [dvb.common.edges.users :as user-edges]
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
    (prop/for-all [user (gen/fmap user-edges/clj->api (s/gen ::user-specs/user))]
                  (= user (validate/validate user user-schema)))))

(defspec generated-clj-plans-can-always-be-transformed-into-valid-api-plans
  (let [plan-schema (-> spec/api serialize/denormalize :components :schemas
                        :Plan)]
    (prop/for-all [plan (gen/fmap plan-edges/clj->api (s/gen ::plan-specs/plan))]
                  (= plan (validate/validate plan plan-schema)))))

(defspec generated-clj-forms-can-always-be-transformed-into-valid-api-forms
  (prop/for-all [form (gen/fmap form-edges/clj->api (s/gen ::form-specs/form))]
                (= form (validate/validate form (:Form common-edges/schemas)))))

(defspec generated-clj-api-keys-can-always-be-transformed-into-valid-api-api-keys
  (prop/for-all [api-key (gen/fmap api-key-edges/clj->api (s/gen ::api-key-specs/api-key))]
                (= api-key (validate/validate api-key (:APIKey common-edges/schemas)))))

(comment

  (def plan (plan-edges/clj->api (gen/generate (s/gen ::plan-specs/plan))))

  plan

  (validate/validate plan (:Plan common-edges/schemas))

)
