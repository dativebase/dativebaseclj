(ns dvb.common.edges.plans
  (:require [dvb.common.edges.common :as common]
            [dvb.common.edges.members-of-plan :as members-of-plan]))

(def pg->clj-coercions
  {:tier keyword
   :members members-of-plan/pgs->cljs})

(def clj->pg-coercions {:tier name})

(def config
  {:pg->clj-coercions pg->clj-coercions
   :clj->pg-coercions clj->pg-coercions
   :fe-db->api-coercions {}
   :api->clj-coercions (merge common/api->clj-coercions
                              pg->clj-coercions
                              {:members members-of-plan/apis->cljs
                               :olds (fn [olds] (mapv keyword olds))})
   :clj->api-coercions (merge (assoc common/clj->api-coercions
                                     :members members-of-plan/cljs->apis
                                     :olds (fn [olds] (mapv name olds)))
                              clj->pg-coercions)
   :resource-schema :Plan})

(def api->clj (partial common/api->clj config))
(def clj->api (partial common/clj->api config))
(def write-clj->api clj->api)
(def pg->clj (partial common/pg->clj config))
(def clj->pg (partial common/clj->pg config))
(def show-api->clj (partial common/show-api->clj api->clj))
