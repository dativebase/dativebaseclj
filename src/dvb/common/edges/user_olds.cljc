(ns dvb.common.edges.user-olds
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as u]))

(def pg->clj-coercions {:role keyword
                        :old-slug keyword})

(def clj->pg-coercions {:role name
                        :old-slug name})

(def config
  {:pg->clj-coercions pg->clj-coercions
   :clj->pg-coercions clj->pg-coercions
   :api->clj-coercions (merge common/api->clj-coercions
                              pg->clj-coercions
                              {:user-id u/str->uuid})
   :clj->api-coercions (merge common/clj->api-coercions
                              clj->pg-coercions
                              {:user-id u/uuid->str})
   :resource-schema :UserOLD
   :resource-write-schema :UserOLDWrite
   :resource-update-schema :UserOLDUpdate})

(def api->clj (partial common/api->clj config))
(def clj->api (partial common/clj->api config))
(def write-clj->api (partial common/write-clj->api config))
(def update-clj->api (partial common/update-clj->api config))
(def clj->pg (partial common/clj->pg config))
(def pg->clj (partial common/pg->clj config))
(def show-api->clj (partial common/show-api->clj api->clj))

(defn fetch-apis->cljs [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body (fn [olds] (map api->clj olds)))
    response))
