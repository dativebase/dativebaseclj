(ns dvb.common.edges.common
  (:require [clojure.set :as set]
            [dvb.common.openapi.spec :as spec]
            [dvb.common.utils :as utils]))

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

;; Functions designed for partial-ing with resource-specific config in order to
;; create edge transformation functions.

(defn fe-db->api
  [{:as _config
    :keys [fe-db->api-coercions resource-schema clj->api-rename-keys]
    :or {clj->api-rename-keys {}}}
   resource]
  (let [keys-to-select (-> schemas resource-schema :properties keys)]
    (cond-> resource
      :always (perform-coercions fe-db->api-coercions)
      :always (set/rename-keys clj->api-rename-keys)
      keys-to-select (select-keys keys-to-select))))

(defn clj->api [{:as _config
                 :keys [resource-schema clj->api-coercions clj->api-rename-keys]
                 :or {clj->api-rename-keys {}}} resource]
  (let [keys-to-select (-> schemas resource-schema :properties keys)]
    (cond-> resource
      :always (perform-coercions clj->api-coercions)
      :always (set/rename-keys clj->api-rename-keys)
      keys-to-select (select-keys keys-to-select))))

(defn write-clj->api [{:as config :keys [resource-write-schema]} resource-write]
  (clj->api (assoc config :resource-schema resource-write-schema) resource-write))

(defn update-clj->api [{:as config :keys [resource-update-schema]} resource-update]
  (clj->api (assoc config :resource-schema resource-update-schema) resource-update))

(defn clj->pg [{:as _config
                :keys [clj->pg-coercions clj->pg-rename-keys]
                :or {clj->pg-rename-keys {} clj->pg-coercions {}}} resource]
  (-> resource
      (perform-coercions clj->pg-coercions)
      (set/rename-keys clj->pg-rename-keys)))

(defn write-fe-db->api
  [{:as _config
    :keys [fe-db->api-coercions clj->api-rename-keys resource-write-schema]
    :or {clj->api-rename-keys {}}}
   resource-write]
  (let [keys-to-select (-> schemas resource-write-schema :properties keys)]
    (cond-> resource-write
      :always (perform-coercions fe-db->api-coercions)
      :always (set/rename-keys clj->api-rename-keys)
      keys-to-select (select-keys keys-to-select))))

(defn api->clj [{:as _config
                 :keys [api->clj-coercions api->clj-rename-keys]
                 :or {api->clj-rename-keys {}}} resource]
  (-> resource
      (set/rename-keys api->clj-rename-keys)
      (perform-coercions api->clj-coercions)))

(defn pg->clj [{:as _config
                :keys [pg->clj-coercions pg->clj-rename-keys]
                :or {pg->clj-coercions {} pg->clj-rename-keys {}}} resource]
  (-> resource
      (set/rename-keys pg->clj-rename-keys)
      (perform-coercions pg->clj-coercions)))

;; The following fns reach into HTTP responses and update the correct part of the
;; expected body, converting API conventions to Clojure ones.

(defn show-api->clj
  [api->clj {:as response :keys [status]}]
  (if (some #{status} [200 201])
    (update response :body api->clj)
    response))

(defn index-api->clj
  [api->clj {:as response :keys [status]}]
  (if (= 200 status)
    (update-in response [:body :data] (partial mapv api->clj))
    response))
