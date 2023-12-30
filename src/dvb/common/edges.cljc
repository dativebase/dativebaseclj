(ns dvb.common.edges
  "Edges means boundaries. We want all entities to be in a Clojure-idiomatic
  format when handled in Clojure code. Other media require other formats. For
  example, PostgreSQL has different conventions than Clojure and the REST API has
  different conventions still. The boundaries we care about are CLJ <=> PG and
  CLJ <=> API.
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
            [dvb.common.openapi.spec :as spec]
            [dvb.common.utils :as utils]))

;; Utilities:

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

;; Users

(def user-clj->api-rename-keys {:is-superuser? :is-superuser})
(def user-api->clj-rename-keys (set/map-invert user-clj->api-rename-keys))
(def user-clj->pg-rename-keys user-clj->api-rename-keys)
(def user-pg->clj-rename-keys user-api->clj-rename-keys)

(def user-pg->clj-coercions {:registration-status keyword})
(def user-clj->pg-coercions {:registration-status name})

(def user-api->clj-coercions
  (merge (assoc api->clj-coercions :registration-key utils/str->uuid)
         user-pg->clj-coercions))

(def user-clj->api-coercions
  (merge (assoc clj->api-coercions :registration-key utils/uuid->str)
         user-clj->pg-coercions))

(defn user-clj->api [user]
  (-> user
      (perform-coercions user-clj->api-coercions)
      (set/rename-keys user-clj->api-rename-keys)
      (select-keys (-> schemas :User :properties keys))))

(defn user-write-clj->api [user-write]
  (-> user-write
      (perform-coercions user-clj->api-coercions)
      (set/rename-keys user-clj->api-rename-keys)
      (select-keys (-> schemas :UserWrite :properties keys))))

(def user-update-clj->api user-write-clj->api)

(defn user-api->clj [user]
  (-> user
      (set/rename-keys user-api->clj-rename-keys)
      (perform-coercions user-api->clj-coercions)))

(defn user-pg->clj [user]
  (-> user
      (set/rename-keys user-pg->clj-rename-keys)
      (perform-coercions user-pg->clj-coercions)))

(defn create-user-api->clj [{:as response :keys [status]}]
  (if (= 201 status)
    (update response :body user-api->clj)
    response))

(defn fetch-user-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body user-api->clj)
    response))

;; Plans

(def plan-pg->clj-coercions {:tier keyword})
(def plan-clj->pg-coercions {:tier name})

(def plan-api->clj-coercions
  (merge api->clj-coercions
         plan-pg->clj-coercions))

(def plan-clj->api-coercions
  (merge clj->api-coercions
         plan-clj->pg-coercions))

(defn plan-api->clj [plan]
  (perform-coercions plan plan-api->clj-coercions))

(defn plan-clj->api [plan]
  (-> plan
      (perform-coercions plan-clj->api-coercions)
      (select-keys (-> schemas :Plan :properties keys))))

(defn plan-pg->clj [plan]
  (perform-coercions plan plan-pg->clj-coercions))

(defn plan-clj->pg [plan]
  (perform-coercions plan plan-clj->pg-coercions))

;; Users-Plans

(def user-plan-pg->clj-coercions {:role keyword})
(def user-plan-clj->pg-coercions {:role name})

(def user-plan-api->clj-coercions
  (merge api->clj-coercions
         user-plan-pg->clj-coercions))

(def user-plan-clj->api-coercions
  (merge clj->api-coercions
         user-plan-clj->pg-coercions))

(defn user-plan-api->clj [user-plan]
  (perform-coercions user-plan user-plan-api->clj-coercions))

(defn user-plan-clj->api [user-plan]
  (perform-coercions user-plan user-plan-clj->api-coercions))

(defn user-plan-pg->clj [user-plan]
  (perform-coercions user-plan user-plan-pg->clj-coercions))

(defn user-plan-clj->pg [user-plan]
  (perform-coercions user-plan user-plan-clj->pg-coercions))

;; API Keys

(def api-key-api->clj-coercions
  (merge api->clj-coercions
         {:expires-at utils/str->instant
          :user-id utils/str->uuid}))

(def api-key-clj->api-coercions
  (merge clj->api-coercions
         {:expires-at utils/instant->str
          :user-id utils/uuid->str}))

(defn api-key-clj->api [api-key]
  (-> api-key
      (perform-coercions api-key-clj->api-coercions)
      (select-keys (-> schemas :APIKey :properties keys))))

(defn api-key-api->clj [api-key]
  (-> api-key
      (perform-coercions api-key-api->clj-coercions)))

;; Forms

(def form-pg->clj-coercions {:old-slug keyword})
(def form-clj->pg-coercions {:old-slug name})

(def form-api->clj-coercions
  (merge api->clj-coercions
         form-pg->clj-coercions))

(def form-clj->api-coercions
  (merge clj->api-coercions
         form-clj->pg-coercions))

(defn form-clj->api [form]
  (-> form
      (perform-coercions form-clj->api-coercions)
      (select-keys (-> schemas :Form :properties keys))))

(defn form-write-clj->api [form-write]
  (-> form-write
      (perform-coercions form-clj->api-coercions)
      (select-keys (-> schemas :FormWrite :properties keys))))

(def form-update-clj->api form-write-clj->api)

(defn form-api->clj [form]
  (-> form
      (perform-coercions form-api->clj-coercions)))

(defn form-pg->clj [form]
  (-> form
      (perform-coercions form-pg->clj-coercions)))

(defn create-form-api->clj [{:as response :keys [status]}]
  (if (= 201 status)
    (update response :body form-api->clj)
    response))

(defn fetch-form-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body form-api->clj)
    response))
