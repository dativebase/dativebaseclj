(ns dvb.common.edges.common
  (:require [dvb.common.openapi.spec :as spec]
            [dvb.common.utils :as utils]))

(def schemas (-> spec/api :components :schemas))

(defn perform-coercions [entity coercions]
  (reduce
   (fn [entity [k coercer]]
     (if (contains? entity k)
       (update entity k coercer)
       entity))
   entity
   coercions))

;; Common coercions: useful for (almost) all entity types:

(def api->clj-coercions
  {:id utils/str->uuid
   :created-by utils/maybe-str->uuid
   :updated-by utils/maybe-str->uuid
   :created-at utils/str->instant
   :updated-at utils/str->instant
   :destroyed-at utils/maybe-str->instant})

(def clj->api-coercions
  {:id utils/uuid->str
   :created-by utils/maybe-uuid->str
   :updated-by utils/maybe-uuid->str
   :created-at utils/instant->str
   :updated-at utils/instant->str
   :destroyed-at utils/maybe-instant->str})
