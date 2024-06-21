(ns dvb.common.edges.api-keys
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as utils]))

(def config
  {:api->clj-coercions (merge common/api->clj-coercions
                              {:expires-at utils/str->instant
                               :user-id utils/str->uuid})
   :clj->api-coercions (merge common/clj->api-coercions
                              {:expires-at utils/instant->str
                               :user-id utils/uuid->str})
   :resource-schema :APIKey})

(def clj->api (partial common/clj->api config))
(def api->clj (partial common/api->clj config))
