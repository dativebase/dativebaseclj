(ns dvb.common.edges.olds-of-user
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as utils]))

(def pg->clj-coercions {:role keyword})

(def clj->pg-coercions {:role name})

(def api->clj-coercions
  (merge common/api->clj-coercions
         {:user-old-id utils/str->uuid}
         pg->clj-coercions))

(def clj->api-coercions
  (merge common/clj->api-coercions
         {:user-old-id utils/uuid->str}
         clj->pg-coercions))

(defn pg->clj [old-of-user]
  (-> old-of-user
      (common/perform-coercions pg->clj-coercions)))

(defn clj->api [old-of-user]
  (-> old-of-user
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :OLDOfUser :properties keys))))

(defn api->clj [old-of-user]
  (-> old-of-user
      (common/perform-coercions api->clj-coercions)))

(defn pgs->cljs [olds-of-user]
  (mapv pg->clj olds-of-user))

(defn cljs->apis [olds-of-user]
  (mapv clj->api olds-of-user))

(defn apis->cljs [olds-of-user]
  (mapv api->clj olds-of-user))
