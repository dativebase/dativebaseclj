(ns dvb.common.edges.plans-of-user
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as utils]))

(def pg->clj-coercions
  {:role keyword
   :tier keyword})

(def clj->pg-coercions
  {:role name
   :tier name})

(def api->clj-coercions
  (merge common/api->clj-coercions
         {:user-plan-id utils/str->uuid}
         pg->clj-coercions))

(def clj->api-coercions
  (merge common/clj->api-coercions
         {:user-plan-id utils/uuid->str}
         clj->pg-coercions))

(defn pg->clj [plan-of-user]
  (-> plan-of-user
      (common/perform-coercions pg->clj-coercions)))

(defn clj->api [plan-of-user]
  (-> plan-of-user
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :PlanOfUser :properties keys))))

(defn api->clj [plan-of-user]
  (-> plan-of-user
      (common/perform-coercions api->clj-coercions)))

(defn pgs->cljs [plans-of-user]
  (mapv pg->clj plans-of-user))

(defn cljs->apis [plans-of-user]
  (mapv clj->api plans-of-user))

(defn apis->cljs [plans-of-user]
  (mapv api->clj plans-of-user))
