(ns dvb.common.edges.user-olds
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as utils]))

(def pg->clj-coercions {:role keyword})

(def clj->pg-coercions {:role name})

(def api->clj-coercions
  (merge common/api->clj-coercions
         pg->clj-coercions
         {:user-id utils/str->uuid}))

(def clj->api-coercions
  (merge common/clj->api-coercions
         clj->pg-coercions
         {:user-id utils/uuid->str}))

(defn api->clj [user-old]
  (common/perform-coercions user-old api->clj-coercions))

(defn clj->api [user-old]
  (common/perform-coercions user-old clj->api-coercions))

(defn pg->clj [user-old]
  (common/perform-coercions user-old pg->clj-coercions))

(defn clj->pg [user-old]
  (common/perform-coercions user-old clj->pg-coercions))

(defn create-api->clj [{:as response :keys [status]}]
  (if (= 201 status)
    (update response :body api->clj)
    response))

(defn fetch-apis->cljs [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body (fn [olds] (map api->clj olds)))
    response))

(defn fetch-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body api->clj)
    response))
