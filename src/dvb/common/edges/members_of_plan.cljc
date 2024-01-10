(ns dvb.common.edges.members-of-plan
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as utils]))

(def pg->clj-coercions {:role keyword})

(def clj->pg-coercions {:role name})

(def api->clj-coercions
  (merge common/api->clj-coercions
         {:user-plan-id utils/str->uuid}
         pg->clj-coercions))

(def clj->api-coercions
  (merge common/clj->api-coercions
         {:user-plan-id utils/uuid->str}
         clj->pg-coercions))

(defn pg->clj [member-of-plan]
  (-> member-of-plan
      (common/perform-coercions pg->clj-coercions)))

(defn clj->api [member-of-plan]
  (-> member-of-plan
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :MemberOfPlan :properties keys))))

(defn api->clj [member-of-plan]
  (-> member-of-plan
      (common/perform-coercions api->clj-coercions)))

(defn pgs->cljs [members-of-plan]
  (mapv pg->clj members-of-plan))

(defn cljs->apis [members-of-plan]
  (mapv clj->api members-of-plan))

(defn apis->cljs [members-of-plan]
  (mapv api->clj members-of-plan))
