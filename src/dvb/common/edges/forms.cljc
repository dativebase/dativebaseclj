(ns dvb.common.edges.forms
  (:require [dvb.common.edges.common :as common]))

(def pg->clj-coercions {:old-slug keyword})

(def clj->pg-coercions {:old-slug name})

(def api->clj-coercions
  (merge common/api->clj-coercions
         pg->clj-coercions))

(def clj->api-coercions
  (merge common/clj->api-coercions
         clj->pg-coercions))

(defn clj->api [form]
  (-> form
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :Form :properties keys))))

(defn clj->pg [form]
  (-> form
      (common/perform-coercions clj->pg-coercions)))

(defn write-clj->api [form-write]
  (-> form-write
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :FormWrite :properties keys))))

(def update-clj->api write-clj->api)

(defn api->clj [form]
  (-> form
      (common/perform-coercions api->clj-coercions)))

(defn pg->clj [form]
  (-> form
      (common/perform-coercions pg->clj-coercions)))

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
