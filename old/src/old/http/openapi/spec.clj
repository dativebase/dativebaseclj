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
             :email "TODO@gmail.com"}})

(def tags
  [{:name :FormsTag
    :description "Operations on forms."}])

(def servers
  [{:url "http://localhost:8080"
    :description "Local development server"}
   {:url "https://api.onlinelinguisticdatabase.org"
    :description "Proposed production server for the OLD HTTP API."}])

(def schemas
  {:EditFormData form/edit-form-data
   :Error error/error
   :ErrorBadRequest400 error/error-bad-request-400
   :ErrorTooManyRequests error/error-too-many-requests
   :ErrorTooManyRequests429 error/error-too-many-requests-429
   :ErrorUnauthorized error/error-unauthorized
   :ErrorUnauthorized401 error/error-unauthorized-401
   :ErrorUnauthorized403 error/error-unauthorized-403
   :ErrorUnrecognizedAcceptHeader error/error-unrecognized-accept-header
   :ErrorUnrecognizedAcceptHeader406 error/error-unrecognized-accept-header-406
   :NewFormData form/new-form-data
   :Form form/form
   :FormWrite form/form-write
   :FormsSearch form/forms-search
   :OLDError error/old-error
   :OLDError500 error/old-error-500
   :OLDUnavailable error/old-unavailable
   :OLDUnavailable503 error/old-unavailable-503
   :PageOfForms form/page-of-forms})

(def uuid-string-regex (str "^"
                            "[a-f0-9]{8}-"
                            "[a-f0-9]{4}-"
                            "[a-f0-9]{4}-"
                            "[a-f0-9]{4}-"
                            "[a-f0-9]{12}$"))

(def parameters
  {:acceptJSONHeaderParam {:name :accept
                           :in :header
                           :description "The content type of the response body that the client will accept. This API requires that `application/json` be supplied here. Otherwise, a 406 response status will be returned."
                           :required true
                           :schema {:type :string
                                    :enum ["application/json"]}}
   :oldSlugPathParam {:name :old_slug
                      :in :path
                      :description "The slug of the target OLD."
                      :required true
                      :schema {:type :string
                               :pattern "^[a-zA-Z0-9_-]+$"}}
   :formIDParam {:name :form_id
                 :in :path
                 :description "The ID of the referenced form."
                 :required true
                 :schema {:type :string
                          :pattern uuid-string-regex}}
   :pageQueryParam {:name :page
                    :in :query
                    :description "The 0-based index of the page of forms requested. The first page is 0."
                    :required false
                    :schema {:type :integer
                             :default 0}}
   :itemsPerPageQueryParam {:name :items-per-page
                            :in :query
                            :description "The maximum number of items to return in each page of forms."
                            :required false
                            :schema {:type :integer
                                     :enum [1 5 10 50]
                                     :default 10}}})

(def security-schemes
  {:x-api-key
   {:type :api-key
    :in :header
    :name "X-API-KEY"}
   :x-app-id
   {:type :api-key
    :in :header
    :name "X-APP-ID"}})

(def security [{:x-api-key []
                :x-app-id []}])

(def common-form-related-responses
  {"401" {:description "The client is not authenticated and therefore cannot perform this operation."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorUnauthorized401"}}}}
   "403" {:description "The client is not authorized to perform this operation."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorUnauthorized403"}}}}
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
          :content {:application-json {:schema {:$ref "#/components/schemas/OLDUnavailable503"}}}}})

(def forms-path
  {:get
   {:operation-id :index-forms
    :summary "Return all forms matching the supplied query."
    :description "Return all forms matching the supplied query and pagination parameters."
    :tags [:FormsTag]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/pageQueryParam"}
                 {:$ref "#/components/parameters/itemsPerPageQueryParam"}]
    :responses
    (assoc common-form-related-responses
           "200" {:description "A page of forms."
                  :content {:application-json {:schema {:$ref "#/components/schemas/PageOfForms"}}}}
           "400" {:description "The request for forms was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :post
   {:operation-id :create-form
    :summary "Create a new form."
    :description "Create a new form then return the created form."
    :tags [:FormsTag]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :request-body
    {:description "The payload to create a form. This payload must conform to the schema FormWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/FormWrite"}}}}
    :responses
    (assoc common-form-related-responses
           "201" {:description "The created form, including server-side-generated values such as the ID."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Form"}}}}
           "400" {:description "The request to create a new form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def form-path
  {:get
   {:operation-id :show-form
    :summary "Return the form with the provided ID."
    :description "Return the form with the provided ID."
    :tags [:FormsTag]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/formIDParam"}]
    :responses
    (assoc common-form-related-responses
           "200" {:description "The form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Form"}}}}
           "400" {:description "The request for a specific form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :delete
   {:operation-id :delete-form
    :summary "Delete the form with the provided ID."
    :description "Delete the form with the provided ID. This is a soft delete. The form data are not actually removed from the database. However, the system will behave as though the form no longer exists."
    :tags [:FormsTag]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/formIDParam"}]
    :responses
    (assoc common-form-related-responses
           "200" {:description "The deleted form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Form"}}}}
           "400" {:description "The request to delete the specified form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :put
   {:operation-id :update-form
    :summary "Update the form with the provided ID."
    :description "Update the form with the provided ID using the JSON payload of the request."
    :tags [:FormsTag]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/formIDParam"}]
    :request-body
    {:description "The payload representing the desired new state of the form. This payload must conform to the schema FormWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/FormWrite"}}}}
    :responses
    (assoc common-form-related-responses
           "200" {:description "The updated form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Form"}}}}
           "400" {:description "The request to update the specified form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def new-form-path
  {:get
   {:operation-id :new-form
    :summary "Return the data needed to create a new form."
    :description "Return the data needed to create a new form."
    :tags [:FormsTag]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :responses
    (assoc common-form-related-responses
           "200" {:description "The data needed to create a new form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/NewFormData"}}}})}})

(def search-forms-path
  {:post
   {:operation-id :search-forms
    :summary "Perform a search over the forms in this OLD."
    :description "Perform a search over the forms in this OLD."
    :tags [:FormsTag]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :request-body
    {:description "The query for searching over a set of forms. This payload must conform to the schema FormsSearch."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/FormsSearch"}}}}
    :responses
    (assoc common-form-related-responses
           "200" {:description "The set of forms matching the search query in the request."
                  :content {:application-json {:schema {:$ref "#/components/schemas/PageOfForms"}}}}
           "400" {:description "The search request was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def edit-form-path
  {:get
   {:operation-id :edit-form
    :summary "Return the data needed to update an existing form."
    :description "Return the data needed to update an existing form."
    :tags [:FormsTag]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/formIDParam"}]
    :responses
    (assoc common-form-related-responses
           "200" {:description "The data needed to update the specified form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/EditFormData"}}}}
           "400" {:description "The request for the data needed to update the specified form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

;; | Purpose         | HTTP Method | Path                     | Method |
;; +=================+=============+==========================+========+
;; | Create new      | POST        | /<cllctn_name>           | create | DONE
;; | Create data     | GET         | /<cllctn_name>/new       | new    | DONE
;; | Read all        | GET         | /<cllctn_name>           | index  | DONE
;; | Read specific   | GET         | /<cllctn_name>/<id>      | show   | DONE
;; | Update specific | PUT         | /<cllctn_name>/<id>      | update | DONE
;; | Update data     | GET         | /<cllctn_name>/<id>/edit | edit   | DONE
;; | Delete specific | DELETE      | /<cllctn_name>/<id>      | delete | DONE
;; | Search          | POST        | /<cllctn_name>/search    | search |

(def paths
  {"/api/v1/{old_slug}/forms/new" new-form-path
   "/api/v1/{old_slug}/forms/search" search-forms-path
   "/api/v1/{old_slug}/forms/{form_id}/edit" edit-form-path
   "/api/v1/{old_slug}/forms/{form_id}" form-path
   "/api/v1/{old_slug}/forms" forms-path})

(def api
  {:components
   {:examples {}
    :parameters parameters
    :schemas schemas
    :security-schemes security-schemes}
   :info info
   :openapi "3.0.0"
   :paths paths
   :security security
   :servers servers
   :tags tags})
