(ns dvb.common.edges.users-of-old
   (:require [dvb.common.edges.common :as common]
             [dvb.common.utils :as utils]))

(def pg->clj-coercions
  {:role keyword})

(def clj->pg-coercions
  {:role name})

(def api->clj-coercions
  (merge common/api->clj-coercions
         {:user-old-id utils/str->uuid}
         pg->clj-coercions))

(def clj->api-coercions
  (merge common/clj->api-coercions
         {:user-old-id utils/uuid->str}
         clj->pg-coercions))

(defn pg->clj [user-of-old]
  (-> user-of-old
      (common/perform-coercions pg->clj-coercions)))

(defn clj->api [user-of-old]
  (-> user-of-old
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :UserOfOLD :properties keys))))

(defn api->clj [user-of-old]
  (-> user-of-old
      (common/perform-coercions api->clj-coercions)))

(defn pgs->cljs [users-of-old]
  (mapv pg->clj users-of-old))

(defn cljs->apis [users-of-old]
  (mapv clj->api users-of-old))

(defn apis->cljs [users-of-old]
  (mapv api->clj users-of-old))
