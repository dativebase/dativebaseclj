(ns dvb.common.openapi.serialize
  "Functions for serializing a Clojure map representing an OpenAPI schema to
   well-formed OpenAPI v3 YAML or JSON. "
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [dvb.common.utils :as u]))

(def openapi-reserved-keywords
  #{:additional-properties
    :api-key
    :authorization-code
    :authorization-url
    :client-credentials
    :min-items
    :max-items
    :max-length
    :one-of
    :operation-id
    :refresh-url
    :request-body
    :security-schemes
    :token-url})

(defn- kebab->camel [k]
  (let [[w1 & ws] (-> k name (str/split #"-"))]
    (apply str w1 (map str/capitalize ws))))

(defn- kebab->snake [k] (str/replace (name k) #"-" "_"))

(defn- clj-kw->openapi-str
  "By default, convert Clojure kebab-case keywords to snake_case. Some
   exceptions, notably that OpenAPI reserved keyword kebab-case keywords are
   converted to camelCase strings."
  [kw]
  (cond  ;; two special cases first
    (= kw :application-json) "application/json"
    (some #{kw} [:date-time :X-API-KEY]) (name kw)
    (openapi-reserved-keywords kw) (kebab->camel kw)
    :else (kebab->snake kw)))

(defn- get-all-references
  "Return the set of strings that are the values of :$ref keys anywhere in the
   OpenAPI Schema map `openapi-spec`."
  [openapi-spec]
  (cond
    (map? openapi-spec)
    (reduce (fn [acc [k v]]
              (if (= :$ref k)
                (conj acc v)
                (set/union acc (get-all-references v))))
            #{}
            openapi-spec)
    (coll? openapi-spec)
    (apply set/union (map get-all-references openapi-spec))
    :else
    #{}))

(defn- get-ref-as-seq
  "Given `'#/a/b/c'`, return `(:a :b :c)`."
  [ref-str]
  (->> (str/split ref-str #"/")
       rest
       (map keyword)))

(defn- get-all-refs-as-seqs [ref-set] (map get-ref-as-seq ref-set))

(defn- ref-seq->str [ref-seq]
  (->> ref-seq
       (map name)
       (str/join "/")
       (str "#/")))

(defn denormalize
  "Return OpenAPI Schema `openapi-spec` with all `{:$ref ...}` maps replaced by their
   references. Denormalize is the antidote to get-all-references."
  ([openapi-spec] (denormalize openapi-spec openapi-spec))
  ([openapi-spec part]
   (cond
     (map? part)
     (if-let [openapi-spec-ref (:$ref part)]
       (denormalize openapi-spec (get-in openapi-spec (get-ref-as-seq openapi-spec-ref)))
       (into {} (map (fn [[k v]] [k (denormalize openapi-spec v)]) part)))
     (coll? part) (into (empty part) (map (partial denormalize openapi-spec) part))
     :else part)))

(defn clj->openapi
  "Convert a Clojure idiomatic data structure to an OpenAPI-conformant one.
   Keyword keys should be strings, kebab-case OpenAPI keywords should be
   camelCase, other kebab-case should be snake_case, etc."
  [clj-ds]
  (cond
    (map? clj-ds) (->> clj-ds
                       (map (fn [[k v]] [(clj->openapi k) (clj->openapi v)]))
                       (into {}))
    (coll? clj-ds) (map clj->openapi clj-ds)
    (keyword? clj-ds) (clj-kw->openapi-str clj-ds)
    :else clj-ds))

(defn validation-errors
  "Return all validation errors for `openapi-schema` as a coll.
   This is not OpenAPI-level validation. At the moment, this just detects
   missing $ref references."
  [openapi-schema]
  (let [ref-seqs (-> openapi-schema get-all-references get-all-refs-as-seqs)
        broken-refs (filter (fn [ref-vec] (not (get-in openapi-schema ref-vec))) ref-seqs)]
    (if (seq broken-refs)
      [(u/format
        "The following OpenAPI references ('$ref' values) were not found: %s"
        (str/join ", " (map ref-seq->str broken-refs)))]
      [])))

(def valid? (comp empty? validation-errors))
