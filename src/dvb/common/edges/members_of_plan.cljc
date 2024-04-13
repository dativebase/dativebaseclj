(ns dvb.common.edges.members-of-plan
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as u]))

(def pg->clj-coercions {:role keyword})
(def clj->pg-coercions {:role name})

(def config
  {:pg->clj-coercions pg->clj-coercions
   :clj->pg-coercions clj->pg-coercions
   :api->clj-coercions (merge common/api->clj-coercions
                              {:user-plan-id u/str->uuid}
                              pg->clj-coercions)
   :clj->api-coercions (merge common/clj->api-coercions
                              {:user-plan-id u/uuid->str}
                              clj->pg-coercions)
   :resource-schema :MemberOfPlan})

(def pg->clj (partial common/pg->clj config))
(def clj->api (partial common/clj->api config))
(def api->clj (partial common/api->clj config))

(defn pgs->cljs [members-of-plan] (mapv pg->clj members-of-plan))
(defn cljs->apis [members-of-plan] (mapv clj->api members-of-plan))
(defn apis->cljs [members-of-plan] (mapv api->clj members-of-plan))
