(ns dvb.server.http.openapi.validate
  "Functionality for validating HTTP request data based on OpenAPI specs.

   This is a thin wrapper around `dvb.common.openapi.validate`. The primary
   difference is that the functions in this ns throw exceptions that can be
   determinsitically converted to HTTP responses. The common ns is intended to
   be used both client-side and server-side.

   Two public functions:

   1. `validate`
   2. `validate-parameters-against-single-spec`

   See the

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
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.common.openapi.validate :as validate]))

(defn validate
  "Validate `value` according to `schema`. The optional `path` parameter is the
   path to the value in 'dot.notation', e.g., 'RequestBody'. It defaults to
   'Root'. Example usage on a value that is valid given the schema:
   => (validate {:a 60} {:type :object :properties {:a {:type :integer}}})
   {:a 60}
   If invalid, an exception is thrown whose `ex-data` fully specifies an HTTP
   400 error response."
  ([value schema] (validate value schema []))
  ([value schema path]
   (try (validate/validate value schema path)
        (catch clojure.lang.ExceptionInfo e
          (let [{:keys [error-code data]} (ex-data e)]
            (throw (errors/error-code->ex-info
                    (or error-code :unexpected-error)
                    (merge {:value value
                            :schema schema
                            :path path}
                           (or data {}))))))
        (catch Exception e
          (throw (errors/error-code->ex-info
                  :unexpected-error
                  {:value value
                   :schema schema
                   :path path}))))))

(defn validate-parameters-against-single-spec
  "Validate map `params` according to a single `spec` from the OpenAPI spec.
   If valid, `params` is returned, with possible coercion. If invalid, an
   exception is thrown whose `ex-data` fully specifies an HTTP error response.
   Happy path example:
   => (validate-parameters-against-single-spec
       {:date '2023-12-25'}
       {:name :date :required false :schema {:type :string :format :date}})
   {:date '2023-12-25'}"
  [params spec]
  (try (validate/validate-parameters-against-single-spec params spec)
       (catch clojure.lang.ExceptionInfo e
         (let [{:keys [error-code data]} (ex-data e)]
           (throw (errors/error-code->ex-info
                   (or error-code :unexpected-error)
                   (merge {:params params
                           :spec spec}
                          (or data {}))))))
       (catch Exception e
         (throw (errors/error-code->ex-info
                 :unexpected-error
                 {:params params
                  :spec spec})))))
