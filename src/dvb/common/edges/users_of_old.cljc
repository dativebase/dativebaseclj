(ns dvb.common.edges.users-of-old
   (:require [dvb.common.edges.common :as common]
             [dvb.common.utils :as u]))

(def pg->clj-coercions {:role keyword})
(def clj->pg-coercions {:role name})

(def config
  {:pg->clj-coercions pg->clj-coercions
   :clj->pg-coercions clj->pg-coercions
   :fe-db->api-coercions {}
   :api->clj-coercions (merge common/api->clj-coercions
                              {:user-old-id u/str->uuid}
                              pg->clj-coercions)
   :clj->api-coercions (merge common/clj->api-coercions
                              {:user-old-id u/uuid->str}
                              clj->pg-coercions)
   :resource-schema :UserOfOLD})

(def pg->clj (partial common/pg->clj config))
(def clj->api (partial common/clj->api config))
(def api->clj (partial common/api->clj config))

(defn pgs->cljs [users-of-old] (mapv pg->clj users-of-old))
(defn cljs->apis [users-of-old] (mapv clj->api users-of-old))
(defn apis->cljs [users-of-old] (mapv api->clj users-of-old))
