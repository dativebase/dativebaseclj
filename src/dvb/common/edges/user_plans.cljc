(ns dvb.common.edges.user-plans
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as utils]))

(def pg->clj-coercions {:role keyword})

(def clj->pg-coercions {:role name})

(def api->clj-coercions
  (merge common/api->clj-coercions
         pg->clj-coercions
         {:user-id utils/str->uuid
          :plan-id utils/str->uuid}))

(def clj->api-coercions
  (merge common/clj->api-coercions
         clj->pg-coercions
         {:user-id utils/uuid->str
          :plan-id utils/uuid->str}))

(defn api->clj [user-plan]
  (common/perform-coercions user-plan api->clj-coercions))

(defn write-clj->api [user-plan-write]
  (-> user-plan-write
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :UserPlanWrite :properties keys))))

(defn update-clj->api [user-plan-update]
  (-> user-plan-update
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :UserPlanUpdate :properties keys))))

(defn clj->api [user-plan]
  (common/perform-coercions user-plan clj->api-coercions))

(defn pg->clj [user-plan]
  (common/perform-coercions user-plan pg->clj-coercions))

(defn clj->pg [user-plan]
  (common/perform-coercions user-plan clj->pg-coercions))

(defn create-api->clj [{:as response :keys [status]}]
  (if (= 201 status)
    (update response :body api->clj)
    response))

(defn fetch-apis->cljs [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body (fn [plans] (map api->clj plans)))
    response))

(defn fetch-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body api->clj)
    response))
