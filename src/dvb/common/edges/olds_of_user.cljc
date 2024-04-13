(ns dvb.common.edges.olds-of-user
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as u]))

(def pg->clj-coercions {:role keyword})
(def clj->pg-coercions {:role name})

(def config
  {:pg->clj-coercions pg->clj-coercions
   :clj->pg-coercions clj->pg-coercions
   :api->clj-coercions (merge common/api->clj-coercions
                              {:user-old-id u/str->uuid}
                              pg->clj-coercions)
   :clj->api-coercions (merge common/clj->api-coercions
                              {:user-old-id u/uuid->str}
                              clj->pg-coercions)
   :resource-schema :OLDOfUser})

(def pg->clj (partial common/pg->clj config))
(def clj->api (partial common/clj->api config))
(def api->clj (partial common/api->clj config))

(defn pgs->cljs [olds-of-user] (mapv pg->clj olds-of-user))
(defn cljs->apis [olds-of-user] (mapv clj->api olds-of-user))
(defn apis->cljs [olds-of-user] (mapv api->clj olds-of-user))
