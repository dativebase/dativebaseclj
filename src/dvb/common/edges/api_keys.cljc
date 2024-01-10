(ns dvb.common.edges.api-keys
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as utils]))

(def api->clj-coercions
  (merge common/api->clj-coercions
         {:expires-at utils/str->instant
          :user-id utils/str->uuid}))

(def clj->api-coercions
  (merge common/clj->api-coercions
         {:expires-at utils/instant->str
          :user-id utils/uuid->str}))

(defn clj->api [api-key]
  (-> api-key
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :APIKey :properties keys))))

(defn api->clj [api-key]
  (-> api-key
      (common/perform-coercions api->clj-coercions)))
