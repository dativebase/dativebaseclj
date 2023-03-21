(ns old.http.openapi.validate
  "Functionality for validating data based on OpenAPI specs. Two public
   functions:

   1. `validate`
   2. `validate-parameters-against-single-spec`

   The `validate` function is intended for validating type-aware maps/objects
   (e.g., a parsed JSON request body) against an OpenAPI schema as data. It
   performs no coercion from string values.

   The `validate-parameters-against-single-spec` function is intended for
   validating a map of parameters (e.g., from headers, query string, or path)
   whose values are (simplistically) assumed to always be strings, which can be
   coerced, according to the schema.

   Example usage of `validate`:

     => (validate {:a 60} {:type :object :properties {:a {:type :integer}}})
     {:a 60}

   Example usage of `validate-parameters-against-single-spec`:

     => (validate-parameters-against-single-spec
         {:date '2023-12-25'}
         {:name :date
          :required false
          :schema {:type :string :format :date}})
     {:date '2023-12-25'}"
  #_(:import [org.apache.commons.validator.routines UrlValidator])
  (:require [clojure.string :as str]
            [old.http.openapi.errors :as errors]))

;; Utilities for doing exception-less error handling in the validation
;; machinery. This is an internal implementation detail. The public validators
;; all return values or throw ExceptionInfo.

(defn maybe-reducer [f]
  (fn [[acc error] item]
    (if error
      [nil error]
      (f acc item))))

(defn maybe-kv-reducer [f]
  (fn [[acc error] k v]
    (if error
      [nil error]
      (f acc k v))))

(defn- bind [f [val err]] (if (nil? err) (f val) [nil err]))

(defmacro err->> [val & fns]
  (let [fns (for [f fns] `(bind ~f))]
    `(->> [~val nil]
          ~@fns)))

(declare validate*)

(defn- absent? [value] (= value ::no-value))

(defn- required-but-absent? [value required] (and required (absent? value)))

(defn- coll-but-not-map? [xs] (and (coll? xs) (not (map? xs))))

(def openapi-type->predicate
  {:string string?
   :number number?
   :integer int?
   :boolean boolean?
   :array coll-but-not-map?
   :object map?})

(defn- parse-int [string] (Integer/valueOf string))

(defn- parse-int-or-nil [string]
  (try (parse-int string) (catch Exception _ nil)))

(defn- parse-double* [string] (Double/valueOf string))

(defn- parse-double-or-nil [string]
  (try (parse-double* string) (catch Exception _ nil)))

(defn- string->number [string]
  (let [coerced (parse-double-or-nil string)]
    (if (number? coerced)
      [coerced nil]
      [nil {:error-code :coercion-to-number-error
            :data {:message (format "Failed to coerce `%s` to a number." string)}}])))

(defn- string->integer [string]
  (let [coerced (parse-int-or-nil string)]
    (if (int? coerced)
      [coerced nil]
      [nil {:error-code :coercion-to-integer-error
            :data {:message (format "Failed to coerce `%s` to an integer." string)}}])))

(defn- string->bool [string]
  (cond (some #{(str/lower-case string)} ["y" "yes" "t" "true" "1"])
        [true nil]
        (some #{(str/lower-case string)} ["n" "no" "f" "false" "0"])
        [false nil]
        :else
        [nil {:error-code :coercion-to-boolean-error
              :data {:message (format "Failed to coerce `%s` to a boolean." string)}}]))

(def openapi-type->string-coercer
  {:number string->number
   :integer string->integer
   :boolean string->bool})

(defn- validate-by-type [{:keys [value path value-type] :as ctx}]
  (if ((value-type openapi-type->predicate) value)
    (assoc ctx :validated value)
    (assoc ctx :error
           {:error-code (keyword (str "invalid-" (name value-type)))
            :data {:message (format "Value `%s` at path %s does not match type %s."
                                    value (name path) (name value-type))}})))

(defn- coerce [{:keys [value-type validated value error path] :as _ctx}]
  (if validated
    [validated nil]
    (if-let [coercer (value-type openapi-type->string-coercer)]
      (let [[coerced err] (coercer value)]
        (if err
          [nil {:error-code :coercion-error
                :data {:message (format "Failed to validate %s by coercing value `%s` of type %s to type %s. Error: %s."
                                        (name path) value (type value) (name value-type) err)}}]
          [coerced nil]))
      [nil error])))

(defn- validate-type-and-coerce [value path {value-type :type}]
  (coerce (validate-by-type {:value value
                             :path path
                             :value-type value-type})))

(defn- validate-type [value path {value-type :type}]
  (let [{:keys [error validated]} (validate-by-type {:value value
                                                     :path path
                                                     :value-type value-type})]
    (if error
      [nil error]
      [validated nil])))

(def name->relation
  {:minimum [<= "Value `%s` at path %s is less than the minimum %s."]
   :maximum [>= "Value `%s` at path %s is greater than the maximum %s."]
   :min-items [<= "Count %s for array value at path %s is less than the minimum %s."]
   :max-items [>= "Count %s for array value at path %s is greater than the maximum %s."]})

(defn- validate-by-numeric-relation
  ([relation-name value path schema]
   (validate-by-numeric-relation relation-name identity value path schema))
  ([relation-name value-prepper value path schema]
   (let [comparandum (relation-name schema)
         [relation error-template] (relation-name name->relation)
         prepped-value (value-prepper value)
         predicate (partial relation comparandum)]
     (if (predicate prepped-value)
       [value nil]
       [nil {:error-code (keyword (str (name relation-name) "-violation"))
             :data {:message (format error-template prepped-value (name path) comparandum)}}]))))

(def validate-minimum (partial validate-by-numeric-relation :minimum))
(def validate-maximum (partial validate-by-numeric-relation :maximum))
(def validate-min-items (partial validate-by-numeric-relation :min-items count))
(def validate-max-items (partial validate-by-numeric-relation :max-items count))

(defn- items-are-unique? [value] (= (count value) (count (set value))))

(defn- validate-unique-items [value path {prescription :unique-items}]
  (if (and prescription (= prescription (items-are-unique? value)))
    [value nil]
    [nil {:error-code :duplicate-items
          :data {:message (format "The items in the array at path %s must be unique." path)}}]))

(defn- validate-has-all-required-properties [value path {:keys [required]}]
  (if-let [missing (seq (filter (fn [p] (not (contains? value p))) required))]
    [nil {:error-code :object-missing-required-properties
          :data {:message (format "The object at path %s is lacking the following required keys: %s."
                                  path (str/join ", " (map name missing)))}}]
    [value nil]))

(defn- validate-additional-properties [value path {:keys [additional-properties]}]
  (reduce
   (maybe-reducer
    (fn [acc [k v]]
      (let [[val err] (validate* v (str path \. k) additional-properties)]
        (if err [nil err] [(assoc acc k val)]))))
   [{} nil]
   value))

(defn- validate-properties [value path {:as _schema :keys [properties]}]
  (if properties
    (reduce-kv
     (maybe-kv-reducer
      (fn [acc property {:keys [default] :as schema}]
        (let [validandum (get value property (or default ::no-value))]
          (if (= validandum ::no-value)
            [acc nil]
            (let [[v err] (validate*
                           validandum
                           (str/join \. (map name [path property]))
                           schema)]
              (if err
                [nil err]
                [(assoc acc property v) nil]))))))
     [{} nil]
     properties)
    [value nil]))

(defn- validate-items [value path {schema :items}]
  (reduce-kv
   (maybe-kv-reducer
    (fn [acc idx item]
      (let [[val err] (validate* item (str/join \. [(name path) (str idx)]) schema)]
        (if err [nil err] [(conj acc val) nil]))))
   [[] nil]
   value))

(defn- validate-enum [value path {:keys [enum]}]
  (if (some #{value} enum)
    [value nil]
    [nil {:error-code :invalid-given-enum
          :data {:message (format "Value `%s` at path %s is not one of the following allowed values %s."
                                  value path (str/join ", " enum))}}]))

(defn- validate-pattern [value path {:keys [pattern]}]
  (if (-> pattern re-pattern (re-find value))
    [value nil]
    [nil {:error-code :invalid-given-pattern
          :data {:message (format "Value `%s` at path %s does not match the regular expression pattern %s."
                                  value path pattern)}}]))

(def timezone-offset-regex
  (re-pattern "(?:Z|[+-](?:[0-1][0-9]|2[0-3]):[0-5][0-9])"))

(def date-core-regex
  (re-pattern
   (str
    ;; date-fullyear
    "[0-9]{4}"
    "-"
    ;; date-month = 2DIGIT ; 01-12
    "(?:0[0-9]|1[0-2])"
    "-"
    ;; date-day = 2DIGIT ; 01-28, 01-29, 01-30, 01-31 based on month/year
    "(?:[0-2][0-9]|3[01])")))

(def date-regex (re-pattern (str "^" date-core-regex "$")))

(def date-time-core-regex
  (re-pattern
   (str
    date-core-regex
    "T"
    ;; time-hour = 2DIGIT ; 00-23
    "(?:[0-1][0-9]|2[0-3])"
    ":"
    ;; time-minute = 2DIGIT ; 00-59
    "[0-5][0-9]"
    ":"
    ;; time-second = 2DIGIT ; 00-58, 00-59, 00-60 based on leap second rules
    "(?:[0-5][0-9]|60)"
    ;; time-secfrac
    "(?:"
    "."
    "[0-9]+"
    ")?")))

(def date-time-regex
  (re-pattern
   (str
    "^"
    date-time-core-regex
    timezone-offset-regex
    "$")))

(def date-time-lax-regex
  (re-pattern
   (str
    "^"
    date-time-core-regex
    timezone-offset-regex
    "?"
    "$")))

(defn- is-valid-date-time? [value] (boolean (re-find date-time-regex value)))

(defn- is-valid-date-time-lax? [value] (boolean (re-find date-time-lax-regex value)))

(defn- is-valid-date? [value] (boolean (re-find date-regex value)))

(defmulti validate-format
  (fn [_ _ {openapi-format :format}] openapi-format))

;; TODO: this requires dep commons-validator "1.5.1"
#_(defmethod validate-format :uri [value path _]
    (if (.isValid (UrlValidator. UrlValidator/ALLOW_LOCAL_URLS) value)
      [value nil]
      [nil {:error-code :invalid-url
            :data {e:message (format "Value `%s` at path %s is not a valid URL."
                                     value path)}}]))

(defmethod validate-format :uri [value _ _] [value nil])

(defmethod validate-format :date-time [value path _]
  (if (is-valid-date-time? value)
    [value nil]
    [nil {:error-code :invalid-date-time
          :data {:message (format "Value `%s` at path %s is not a valid date time as per RFC 3339, section 5.6."
                                  value (name path))}}]))

(defmethod validate-format :date-time-lax [value path _]
  (if (is-valid-date-time-lax? value)
    [value nil]
    [nil {:error-code :invalid-date-time
          :data {:message (format "Value `%s` at path %s is not a valid 'lax' date time as per RFC 3339, section 5.6."
                                  value (name path))}}]))

(defmethod validate-format :date [value path _]
  (if (is-valid-date? value)
    [value nil]
    [nil {:error-code :invalid-date
          :data {:message (format "Value `%s` at path %s is not a valid date as per RFC 3339, section 5.6."
                                  value (name path))}}]))

(defn- is-valid-uuid-string? [string]
  (try (java.util.UUID/fromString string)
       true
       (catch Exception _ false)))

(defmethod validate-format :uuid [value path _]
  (if (is-valid-uuid-string? value)
    [value nil]
    [nil {:error-code :invalid-uuid
          :data {:message (format "Value `%s` at path %s is not a valid UUID."
                                  value (name path))}}]))

(defmethod validate-format :default [value _path _] [value nil])

(def scalar-validators
  [[#(contains? % :enum) validate-enum]])

(def string-validators
  (concat [[#(contains? % :pattern) validate-pattern]
           [#(contains? % :format) validate-format]]
          scalar-validators))

(def number-validators
  (concat [[#(contains? % :minimum) validate-minimum]
           [#(contains? % :maximum) validate-maximum]]
          scalar-validators))

(def array-validators
  [[#(contains? % :min-items) validate-min-items]
   [#(contains? % :max-items) validate-max-items]
   [#(contains? % :unique-items) validate-unique-items]])

(defn- get-validators-by-predicates [schema predicates]
  (->> predicates
       (filter (fn [[predicate _]] (predicate schema)))
       (map second)))

(defmulti get-validators :type)

(defmethod get-validators :integer [schema]
  (get-validators-by-predicates schema number-validators))

(defmethod get-validators :number [schema]
  (get-validators-by-predicates schema number-validators))

(defmethod get-validators :string [schema]
  (get-validators-by-predicates schema string-validators))

(defmethod get-validators :boolean [_schema]
  ())

(defmethod get-validators :array [schema]
  (concat (get-validators-by-predicates schema array-validators)
          [validate-items]))

(defmethod get-validators :object [{:as _schema :keys [additional-properties]}]
  (if additional-properties
    [validate-additional-properties]
    [validate-has-all-required-properties
     validate-properties]))

(defmethod get-validators :default [schema]
  (throw (errors/error-code->ex-info
          :unrecognized-data-type-in-openapi-schema
          {:type (:type schema)})))

(defn- validator-closure [path schema]
  (fn [value validator]
    (validator value path schema)))

(defn- aggregate-validations [combinatoric-kw {passes :true fails :false}]
  (if ((case combinatoric-kw
         :one-of (fn [passes _] (= 1 (count passes)))
         :all-of (fn [_ fails] (empty? fails))
         :any-of (fn [passes _] (seq passes)))
       passes
       fails)
    (first passes)
    (if-let [fail (first fails)]
      fail
      (if (= :one-of combinatoric-kw)
        [nil {:error-code :one-of-validator-matches-multiple}]
        [nil {:error-code :complex-validation-error}]))))

(defn- combine-validations [combinatoric-kw validations]
  (->> validations
       (group-by (comp keyword str nil? second))
       (aggregate-validations combinatoric-kw)))

(defn- validate-against-many-schemas
  [combinatoric-kw value path schemas opts]
  (->> schemas
       (map (fn [schema] (validate* value path schema opts)))
       (combine-validations combinatoric-kw)))

(defn- validate-via-complex-schema
  [value path {:keys [one-of all-of any-of]} opts]
  (validate-against-many-schemas
   (cond one-of :one-of
         all-of :all-of
         any-of :any-of)
   value
   path
   (some identity [one-of all-of any-of])
   opts))

(defn- complex? [schema] (some #{:one-of :all-of :any-of} (keys schema)))

(defn- validate*
  ([value path schema] (validate* value path schema {}))
  ([value path {:keys [nullable] :as schema}
    {:as opts :keys [coerce?] :or {coerce? false}}]
   (if (complex? schema)
     (validate-via-complex-schema value path schema opts)
     (if (and nullable (nil? value))
       [value nil]
       (reduce (maybe-reducer (validator-closure path schema))
               [value nil]
               (cons (if coerce? validate-type-and-coerce validate-type)
                     (get-validators schema)))))))

(defn validate
  "Validate `value` according to `schema`. The optional `path` parameter is the
   path to the value in 'dot.notation', e.g., 'RequestBody'. It defaults to
   'Root'. Example usage on a value that is valid given the schema:

     => (validate {:a 60} {:type :object :properties {:a {:type :integer}}})
     {:a 60}

   If invalid, an exception is thrown whose `ex-data` fully specifies an HTTP
   400 error response."
  ([value schema] (validate value schema "Root"))
  ([value schema path]
   (let [[val err] (validate* value path schema)]
     (if err
       (throw (errors/error-code->ex-info
               (:error-code err)
               (or (:data err) nil)))
       val))))

(defn validate-parameters-against-single-spec
  "Validate map `params` according to a single `spec` from the OpenAPI spec.
   If valid, `params` is returned, with possible coercion. If invalid, an
   exception is thrown whose `ex-data` fully specifies an HTTP error response.
   Happy path example:
     => (validate-parameters-against-single-spec
         {:date '2023-12-25'}
         {:name :date :required false :schema {:type :string :format :date}})
     {:date '2023-12-25'}"
  [params {:as _spec param-name :name :keys [required in]
           {:keys [default] :as schema} :schema}]
  (let [in (case in :query "query" (name in))
        value (get params param-name (or default ::no-value))]
    (cond
      (required-but-absent? value required) (throw (errors/error-code->ex-info
                                                    :required-parameter-absent
                                                    {:parameter-name param-name
                                                     :in in}))
      (absent? value) {}
      :else (let [[val err] (validate* value param-name schema {:coerce? true})]
              (if err
                (throw (errors/error-code->ex-info
                        (:error-code err)
                        (merge (or (:data err) nil) {:parameter-name param-name :in in})))
                {param-name val})))))
