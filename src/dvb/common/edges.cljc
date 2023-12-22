(ns dvb.common.edges
  "Edges means boundaries. We want all entities to be in a Clojure-idiomatic
  format when handled in Clojure code. Other media require other formats. For
  example, PostgreSQL has different conventions than Clojure and the REST API has
  different conventions still.
  Clojure conventions:
  - Use keywords for enum values (e.g., status)
  - All keys are kebab-case keywords with punctuation possible.
  - Dates and timestamps are Java time objects and UUIDs are UUID objects.
  API conventions:
  - Use strings for enum values
  - Keys are snake_case strings without punctuation.
  - Dates and timestamps are ISO-formatted strings and UUIDs are strings.
  PostgreSQL conventions:
  - Use strings for enum values
  - Keys are snake_case strings without punctuation.
  - Dates and timestamps are JDBC objects. UUIDs are UUID objects (I believe)."
  (:require [clojure.set :as set]
            [dvb.common.utils :as utils]))

(defn perform-coercions [entity coercions]
  (reduce
   (fn [entity [k coercer]]
     (if (contains? entity k)
       (update entity k coercer)
       entity))
   entity
   coercions))

;; Users

(def user-clj->api-mapping
  {:is-superuser? :is-superuser})

(def user-clj->pg-mapping
  {:is-superuser? :is-superuser})

(def user-api->clj-coercions
  {:id utils/->uuid
   :registration-key utils/->uuid
   :created-at utils/->instant
   :updated-at utils/->instant
   :destroyed-at utils/->nilable-instant})

(def user-pg->clj-coercions
  {:registration-status keyword})

(defn user-clj->api [user]
  (set/rename-keys user user-clj->api-mapping))

(defn user-api->clj [user]
  (-> user
      (set/rename-keys (set/map-invert user-clj->api-mapping))
      (perform-coercions user-api->clj-coercions)))

(defn user-pg->clj [user]
  (-> user
      (set/rename-keys (set/map-invert user-clj->pg-mapping))
      (perform-coercions user-pg->clj-coercions)))

(defn create-user-api->clj [{:as response :keys [status]}]
  (if (= 201 status)
    (update response :body user-api->clj)
    response))

(defn fetch-user-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body user-api->clj)
    response))
