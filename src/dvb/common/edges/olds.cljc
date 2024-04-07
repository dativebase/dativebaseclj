(ns dvb.common.edges.olds
  (:require [dvb.common.edges.common :as common]
            [dvb.common.edges.users-of-old :as users-of-old]
            [dvb.common.utils :as utils]))

(def pg->clj-coercions
  {:users users-of-old/pgs->cljs
   :slug keyword})

(def clj->pg-coercions
  {:slug name})

(def api->clj-coercions
  (merge common/api->clj-coercions
         {:users users-of-old/apis->cljs
          :plan-id utils/maybe-str->uuid}))

(def clj->api-coercions
  (assoc common/clj->api-coercions
         :plan-id utils/maybe-uuid->str
         :users users-of-old/cljs->apis))

(defn clj->api [old]
  (-> old
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :OLD :properties keys))))

(defn write-clj->api [old-write]
  (-> old-write
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :OLDWrite :properties keys))))

(defn update-clj->api [old-update]
  (-> old-update
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :OLDUpdate :properties keys))))

(defn api->clj [old]
  (-> old
      (common/perform-coercions api->clj-coercions)))

(defn pg->clj [old]
  (common/perform-coercions old pg->clj-coercions))

(defn clj->pg [old]
  (common/perform-coercions old clj->pg-coercions))

(defn create-api->clj [{:as response :keys [status]}]
  (if (= 201 status)
    (update response :body api->clj)
    response))

(defn fetch-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body api->clj)
    response))

(defn index-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update-in response [:body :data] (partial mapv api->clj))
    response))
