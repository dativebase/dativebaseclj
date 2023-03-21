(ns old.http.openapi.spec
  "Specification of the OLD HTTP API that conforms to the OpenAPI 3.0
   standard. See https://swagger.io/docs/specification/.

   The `api` map should conform to the OpenAPI specification. To facilitate
   Clojure idiomaticity, OpenAPI reserved string keys like `'operationId'` may
   be represented in the API map as kebab-case keywords, e.g., `:operation-id`.
   Otherwise, keyword keys that are not OpenAPI reserved words are converted to
   `'snake_case'` strings during serialization. The function responsible for
   this transformation is `openapi.serialize/clj->openapi`. In general, you
   should be able to use kebab-case keywords throughout this spec and they will
   be converted to valid OpenAPI YAML during serialization.

   Ideally, the `api` data structure should fully specify the interface,
   including paths, operations, requests, responses, authentication, and servers.
   This API map should be the source of truth for all of these concerns as far
   as is possible, given the OpenAPI spec.

   References:

   - Data Types: https://swagger.io/docs/specification/data-models/data-types/.
   - Request bodies: https://swagger.io/docs/specification/describing-request-body/
   - Response bodies: https://swagger.io/docs/specification/describing-responses/
   - Security: https://swagger.io/docs/specification/authentication/
   - Paths & Operations: https://swagger.io/docs/specification/paths-and-operations/
   - Examples: https://swagger.io/docs/specification/adding-examples/
   - Servers: https://swagger.io/docs/specification/api-host-and-base-path/
   - Parameters: https://swagger.io/docs/specification/describing-parameters/
   - Serialization: https://swagger.io/docs/specification/serialization/"
  (:require [old.http.openapi.spec.components.form :as form]
            [old.http.openapi.spec.components.error :as error]))

(def info
  {:title "Online Linguistic Database (OLD) HTTP API"
   :description "The OLD HTTP API is a RESTful HTTP API that communicates using JSON. This API allows developers to build systems that interact with OLD web services."
   :version "0.1.0"
   :contact {:name "OLD API Support"
             :url "TODO"
             :email "TODO"}})

(def tags
  [{:name :FormsTag
    :description "Operations on forms."}])

(def servers
  [{:url "http://localhost:8080"
    :description "Local development server"}
   {:url "https://api.onlinelinguisticdatabase.org"
    :description "Proposed production server for the OLD HTTP API."}])

(def schemas
  {:Error error/error
   :ErrorBadRequest400 error/error-bad-request-400
   :ErrorTooManyRequests error/error-too-many-requests
   :ErrorTooManyRequests429 error/error-too-many-requests-429
   :ErrorUnauthorized error/error-unauthorized
   :ErrorUnauthorized401 error/error-unauthorized-401
   :ErrorUnrecognizedAcceptHeader error/error-unrecognized-accept-header
   :ErrorUnrecognizedAcceptHeader406 error/error-unrecognized-accept-header-406
   :Form form/form
   :FormWrite form/form-write
   :OLDError error/old-error
   :OLDError500 error/old-error-500
   :OLDUnavailable error/old-unavailable
   :OLDUnavailable503 error/old-unavailable-503
   :PageOfForms form/page-of-forms})

;; TODO: I suspect we may not need to run API key authentication on our end at
;; all, so this may need to be removed or revised.
(def security-schemes
  {:api-key-auth
   {:type :api-key
    :in :header
    :name "X-API-KEY"}})

(def security [{:api-key-auth []}])

(def forms-path
  {:get
   {:operation-id :index-forms
    :summary "Return all forms matching the supplied query."
    :description "Return all forms matching the supplied query. TODO."
    :tags [:FormsTag]
    :parameters [{:name :accept
                  :in :header
                  :description "The content type of the response body that the client will accept. This API requires that `application/json` be supplied here. Otherwise, a 406 response status will be returned."
                  :required true
                  :schema {:type :string
                           :enum ["application/json"]}}
                 {:name :pagination-token
                  :in :query
                  :description "The pagination token. This token opaquely references a page of forms."
                  :required false
                  :schema {:type :string
                           :description "A OLD-generated pagination token, which opaquely specifies a page of forms."}}]
    :responses
    {"200" {:description "A page of forms."
            :content {:application-json {:schema {:$ref "#/components/schemas/PageOfForms"}}}}
     "400" {:description "The request for forms was invalid."
            :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}}
     "401" {:description "The client is not authenticated and therefore cannot access this resource."
            :content {:application-json {:schema {:$ref "#/components/schemas/ErrorUnauthorized401"}}}}
     "406" {:description "Either no `Accept` header was provided or the provided header is not recognized. The only currently recognized accept header is `application/json`."
            :content {:application-json {:schema {:$ref "#/components/schemas/ErrorUnrecognizedAcceptHeader406"}}}}
     "429" {:description "Too many requests. This client has exceeded this API's request rate limit. The client may retry after the period specified in the response and the X header."
            :headers {:Retry-After {:description "The number of seconds that the client must wait before making further requests to this API."
                                    :schema {:type :integer
                                             :minimum 0}
                                    :example 60}}
            :content {:application-json {:schema {:$ref "#/components/schemas/ErrorTooManyRequests429"}}}}
     "500" {:description "There was an error in OLD while attempting to respond to the request. The operators of this service will be alerted to the issue and will address it in a timely manner."
            :content {:application-json {:schema {:$ref "#/components/schemas/OLDError500"}}}}
     "503" {:description "The OLD is unavailable."
            :content {:application-json {:schema {:$ref "#/components/schemas/OLDUnavailable503"}}}}}}})

(def paths
  {"/api/v1/forms" forms-path})

(def api
  {:components
   {:examples {}
    :parameters {}
    :schemas schemas
    :security-schemes security-schemes}
   :info info
   :openapi "3.0.0"
   :paths paths
   :security security
   :servers servers
   :tags tags})
