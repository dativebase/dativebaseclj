(ns dvb.common.edges.old-access-requests
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as u]))

(def pg->clj-coercions {:status keyword
                        :old-slug keyword})

(def clj->pg-coercions {:status name
                        :old-slug name})

(def config
  {:pg->clj-coercions pg->clj-coercions
   :clj->pg-coercions clj->pg-coercions
   :api->clj-coercions (merge common/api->clj-coercions
                              {:user-id u/str->uuid}
                              pg->clj-coercions)
   :clj->api-coercions (merge common/clj->api-coercions
                              {:user-id u/uuid->str}
                              clj->pg-coercions)
   :resource-schema :OLDAccessRequest
   :resource-write-schema :OLDAccessRequestWrite})

(def api->clj (partial common/api->clj config))
(def clj->api (partial common/clj->api config))
(def pg->clj (partial common/pg->clj config))
(def clj->pg (partial common/clj->pg config))
(def write-clj->api (partial common/write-clj->api config))
(def show-api->clj (partial common/show-api->clj api->clj))
(def index-api->clj (partial common/index-api->clj api->clj))

(defn index-for-old-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body (partial mapv api->clj))
    response))
