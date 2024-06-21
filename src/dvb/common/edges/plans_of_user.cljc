(ns dvb.common.edges.plans-of-user
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as u]))

(def pg->clj-coercions {:role keyword
                        :tier keyword})

(def clj->pg-coercions {:role name
                        :tier name})

(def config
  {:pg->clj-coercions pg->clj-coercions
   :clj->pg-coercions clj->pg-coercions
   :api->clj-coercions (merge common/api->clj-coercions
                              {:user-plan-id u/str->uuid}
                              pg->clj-coercions)
   :clj->api-coercions (merge common/clj->api-coercions
                              {:user-plan-id u/uuid->str}
                              clj->pg-coercions)
   :resource-schema :PlanOfUser})

(def pg->clj (partial common/pg->clj config))
(def clj->api (partial common/clj->api config))
(def api->clj (partial common/api->clj config))

(defn pgs->cljs [plans-of-user] (mapv pg->clj plans-of-user))
(defn cljs->apis [plans-of-user] (mapv clj->api plans-of-user))
(defn apis->cljs [plans-of-user] (mapv api->clj plans-of-user))
