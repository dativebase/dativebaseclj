(ns dvb.common.edges
  (:require [clojure.set :as set]
            [dvb.common.utils :as utils]))

(def user-mapping
  {:is-superuser? :is-superuser})

(def user-coercions
  {:id utils/->uuid
   :created-at utils/->instant
   :updated-at utils/->instant
   :destroyed-at utils/->nilable-instant})

(defn perform-coercions [entity coercions]
  (reduce
   (fn [entity [k coercer]]
     (if (contains? entity k)
       (update entity k coercer)
       entity))
   entity
   coercions))

(defn user-clj->api [user]
  (set/rename-keys user user-mapping))

(defn user-api->clj [user]
  (-> user
      (set/rename-keys (set/map-invert user-mapping))
      (perform-coercions user-coercions)))

(defn create-user-api->clj [{:as response :keys [status]}]
  (if (= 201 status)
    (update response :body user-api->clj)
    response))

(defn fetch-user-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body user-api->clj)
    response))
