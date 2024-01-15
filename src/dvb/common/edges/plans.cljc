(ns dvb.common.edges.plans
  (:require [dvb.common.edges.common :as common]
            [dvb.common.edges.members-of-plan :as members-of-plan]))

(def pg->clj-coercions
  {:tier keyword
   :members members-of-plan/pgs->cljs})

(def clj->pg-coercions {:tier name})

(def api->clj-coercions
  (merge common/api->clj-coercions
         pg->clj-coercions
         {:members members-of-plan/apis->cljs}))

(def clj->api-coercions
  (merge (assoc common/clj->api-coercions
                :members members-of-plan/cljs->apis)
         clj->pg-coercions))

(defn api->clj [plan]
  (common/perform-coercions plan api->clj-coercions))

(defn clj->api [plan]
  (-> plan
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :Plan :properties keys))))

(def write-clj->api clj->api)

(defn pg->clj [plan]
  (common/perform-coercions plan pg->clj-coercions))

(defn clj->pg [plan]
  (common/perform-coercions plan clj->pg-coercions))

(defn create-api->clj [{:as response :keys [status]}]
  (if (= 201 status)
    (update response :body api->clj)
    response))

(defn fetch-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body api->clj)
    response))
