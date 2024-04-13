(ns dvb.common.edges.olds
  (:require [dvb.common.edges.common :as common]
            [dvb.common.edges.users-of-old :as users-of-old]
            [dvb.common.utils :as u]))

(def pg->clj-coercions {:users users-of-old/pgs->cljs
                        :slug keyword})

(def clj->pg-coercions {:slug name})

(def config
  {:pg->clj-coercions pg->clj-coercions
   :clj->pg-coercions clj->pg-coercions
   :api->clj-coercions (merge common/api->clj-coercions
                              {:users users-of-old/apis->cljs
                               :slug keyword
                               :plan-id u/maybe-str->uuid})
   :clj->api-coercions (assoc common/clj->api-coercions
                              :plan-id u/maybe-uuid->str
                              :slug u/name-keyword-or-identity
                              :users users-of-old/cljs->apis)
   :resource-schema :OLD
   :resource-write-schema :OLDWrite
   :resource-update-schema :OLDUpdate})

(def clj->api (partial common/clj->api config))
(def write-clj->api (partial common/write-clj->api config))
(def update-clj->api (partial common/update-clj->api config))
(def api->clj (partial common/api->clj config))
(def clj->pg (partial common/clj->pg config))
(def pg->clj (partial common/pg->clj config))
(def show-api->clj (partial common/show-api->clj api->clj))
(def index-api->clj (partial common/index-api->clj api->clj))
