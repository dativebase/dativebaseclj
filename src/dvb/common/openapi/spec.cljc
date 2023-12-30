(ns dvb.common.openapi.spec
  "Specification of the DativeBase HTTP API that conforms to the OpenAPI 3.0
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
  (:require [dvb.common.openapi.spec.components.api-key :as api-key]
            [dvb.common.openapi.spec.components.error :as error]
            [dvb.common.openapi.spec.components.form :as form]
            [dvb.common.openapi.spec.components.login :as login]
            [dvb.common.openapi.spec.components.plan :as plan]
            [dvb.common.openapi.spec.components.user :as user]))

(def info
  {:title "DativeBase HTTP API"
   :description "The DativeBase HTTP API is a RESTful HTTP API that communicates using JSON. This API allows developers to build systems that interact with DativeBase."
   :version "0.1.0"
   :contact {:name "DativeBase API Support"
             :url "TODO"
             :email "TODO@gmail.com"}})

(def tags
  [{:name :Authentication
    :description "Operations related to authentication."}
   {:name :Forms
    :description "Operations on forms."}
   {:name :Users
    :description "Operations on users."}])

(def servers
  [{:url "http://localhost:8080"
    :description "Local development server"
    :id :local}
   {:url "http://localhost:8087"
    :description "Local test server"
    :id :local-test}
   {:url "https://api.onlinelinguisticdatabase.org"
    :description "Proposed production server for the DativeBase HTTP API."
    :id :prod}])

;; UserAndAPIKey
(def user-and-api-key
  {:type :object
   :properties
   {:user {:$ref "#/components/schemas/User"}
    :api-key {:$ref "#/components/schemas/APIKey"}}
   :required [:user
              :api-key]})

(def schemas
  {:APIKey api-key/api-key
   :EditFormData form/edit-form-data
   :EditUserData user/edit-user-data
   :Error error/error
   :ErrorBadRequest400 error/error-bad-request-400
   :ErrorNotFound error/error-not-found
   :ErrorNotFound404 error/error-not-found-404
   :ErrorTooManyRequests error/error-too-many-requests
   :ErrorTooManyRequests429 error/error-too-many-requests-429
   :ErrorUnauthorized error/error-unauthorized
   :ErrorUnauthorized401 error/error-unauthorized-401
   :ErrorUnauthorized403 error/error-unauthorized-403
   :ErrorUnrecognizedAcceptHeader error/error-unrecognized-accept-header
   :ErrorUnrecognizedAcceptHeader406 error/error-unrecognized-accept-header-406
   :ErrorServer error/error-server
   :ErrorServer500 error/error-server-500
   :ErrorUnavailable error/error-unavailable
   :ErrorUnavailable503 error/error-unavailable-503
   :Form form/form
   :FormsSearch form/forms-search
   :FormWrite form/form-write
   :Login login/login
   :NewFormData form/new-form-data
   :NewUserData user/new-user-data
   :PageOfForms form/page-of-forms
   :PageOfUsers user/page-of-users
   :Plan plan/plan
   :PlanWrite plan/plan-write
   :User user/user
   :UserAndAPIKey user-and-api-key
   :UserUpdate user/user-update
   :UserWrite user/user-write})

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
   :planIDParam {:name :plan_id
                 :in :path
                 :description "The ID of the referenced plan."
                 :required true
                 :schema {:type :string
                          :pattern uuid-string-regex}}
   :userIDParam {:name :user_id
                 :in :path
                 :description "The ID of the referenced user."
                 :required true
                 :schema {:type :string
                          :pattern uuid-string-regex}}
   :userRegistrationKeyParam {:name :user_registration_key
                              :in :path
                              :description "The registration key for the user that is being activated."
                              :required true
                              :schema {:type :string
                                       :pattern uuid-string-regex}}
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

(def common-path-responses
  {"401" {:description "The client is not authenticated and therefore cannot perform this operation."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorUnauthorized401"}}}}
   "403" {:description "The client is not authorized to perform this operation."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorUnauthorized403"}}}}
   "404" {:description "The operation or endpoint of the request was not found. The server does not recognize the path of the requests or the path is recognized but the method is not."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorNotFound404"}}}}
   "406" {:description "Either no `Accept` header was provided or the provided header is not recognized. The only currently recognized accept header is `application/json`."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorUnrecognizedAcceptHeader406"}}}}
   "429" {:description "Too many requests. This client has exceeded this API's request rate limit. The client may retry after the period specified in the response and the X header."
          :headers {:Retry-After {:description "The number of seconds that the client must wait before making further requests to this API."
                                  :schema {:type :integer
                                           :minimum 0}
                                  :example 60}}
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorTooManyRequests429"}}}}
   "500" {:description "There was an error in DativeBase while attempting to respond to the request. The operators of this service will be alerted to the issue and will address it in a timely manner."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorServer500"}}}}
   "503" {:description "The DativeBase is unavailable."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorUnavailable503"}}}}})

(def forms-path
  {:get
   {:operation-id :index-forms
    :summary "Return all forms matching the supplied query."
    :description "Return all forms matching the supplied query and pagination parameters."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/pageQueryParam"}
                 {:$ref "#/components/parameters/itemsPerPageQueryParam"}]
    :responses
    (assoc common-path-responses
           "200" {:description "A page of forms."
                  :content {:application-json {:schema {:$ref "#/components/schemas/PageOfForms"}}}}
           "400" {:description "The request for forms was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :post
   {:operation-id :create-form
    :summary "Create a new form."
    :description "Create a new form then return the created form."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :request-body
    {:description "The payload to create a form. This payload must conform to the schema FormWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/FormWrite"}}}}
    :responses
    (assoc common-path-responses
           "201" {:description "The created form, including server-side-generated values such as the ID."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Form"}}}}
           "400" {:description "The request to create a new form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def form-path
  {:get
   {:operation-id :show-form
    :summary "Return the form with the provided ID."
    :description "Return the form with the provided ID."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/formIDParam"}]
    :responses
    (assoc common-path-responses
           "200" {:description "The form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Form"}}}}
           "400" {:description "The request for a specific form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :delete
   {:operation-id :delete-form
    :summary "Delete the form with the provided ID."
    :description "Delete the form with the provided ID. This is a soft delete. The form data are not actually removed from the database. However, the system will behave as though the form no longer exists."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/formIDParam"}]
    :responses
    (assoc common-path-responses
           "200" {:description "The deleted form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Form"}}}}
           "400" {:description "The request to delete the specified form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :put
   {:operation-id :update-form
    :summary "Update the form with the provided ID."
    :description "Update the form with the provided ID using the JSON payload of the request."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/formIDParam"}]
    :request-body
    {:description "The payload representing the desired new state of the form. This payload must conform to the schema FormWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/FormWrite"}}}}
    :responses
    (assoc common-path-responses
           "200" {:description "The updated form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Form"}}}}
           "400" {:description "The request to update the specified form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def new-form-path
  {:get
   {:operation-id :new-form
    :summary "Return the data needed to create a new form."
    :description "Return the data needed to create a new form."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :responses
    (assoc common-path-responses
           "200" {:description "The data needed to create a new form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/NewFormData"}}}})}})

(def search-forms-path
  {:post
   {:operation-id :search-forms
    :summary "Perform a search over the forms in this OLD."
    :description "Perform a search over the forms in this OLD."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :request-body
    {:description "The query for searching over a set of forms. This payload must conform to the schema FormsSearch."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/FormsSearch"}}}}
    :responses
    (assoc common-path-responses
           "200" {:description "The set of forms matching the search query in the request."
                  :content {:application-json {:schema {:$ref "#/components/schemas/PageOfForms"}}}}
           "400" {:description "The search request was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def edit-form-path
  {:get
   {:operation-id :edit-form
    :summary "Return the data needed to update an existing form."
    :description "Return the data needed to update an existing form."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/formIDParam"}]
    :responses
    (assoc common-path-responses
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

(def login-path
  {:post
   {:operation-id :login
    :summary "Login and create a temporary API key"
    :description "Login and create a temporary API key"
    :tags [:Authentication]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}]
    :security [] ;; no security on purpose
    :request-body
    {:description "The payload to login. This payload must conform to the schema Login."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/Login"}}}}
    :responses
    (assoc common-path-responses
           "200" {:description "Successful login request. The authenticated user and a newly-generated API key are both returned in the response. Subsequent requests to other endpoints must supply a valid API ID and key in the request in order to authenticate."
                  :content {:application-json {:schema {:$ref "#/components/schemas/UserAndAPIKey"}}}}
           "400" {:description "The request to login was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def activate-user-path
  {:get
   {:operation-id :activate-user
    :summary "Activate a user"
    :description "Activate a user"
    :tags [:Authentication]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/userIDParam"}
                 {:$ref "#/components/parameters/userRegistrationKeyParam"}]
    :security [] ;; no security on purpose
    :responses
    (assoc common-path-responses
           "200" {:description "Successful user activation request. The user has been fully created. The user's registration status has been changed from 'pending' to 'registered'."
                  :content {:application-json {:schema {:$ref "#/components/schemas/User"}}}}
           "400" {:description "The request to activate the user was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def user-path
  {:get
   {:operation-id :show-user
    :summary "Return the user with the provided ID."
    :description "Return the user with the provided ID."
    :tags [:Users]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/userIDParam"}]
    :responses
    (assoc common-path-responses
           "200" {:description "The user."
                  :content {:application-json {:schema {:$ref "#/components/schemas/User"}}}}
           "400" {:description "The request for a specific user was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :delete
   {:operation-id :delete-user
    :summary "Delete the user with the provided ID."
    :description "Delete the user with the provided ID. This is a soft delete. The user data are not actually removed from the database. However, the system will behave as though the user no longer exists."
    :tags [:Users]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/userIDParam"}]
    :responses
    (assoc common-path-responses
           "200" {:description "The deleted user."
                  :content {:application-json {:schema {:$ref "#/components/schemas/User"}}}}
           "400" {:description "The request to delete the specified user was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :put
   {:operation-id :update-user
    :summary "Update the user with the provided ID."
    :description "Update the user with the provided ID using the JSON payload of the request."
    :tags [:Users]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/userIDParam"}]
    :request-body
    {:description "The payload representing the desired new state of the user. This payload must conform to the schema UserUpdate."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/UserUpdate"}}}}
    :responses
    (assoc common-path-responses
           "200" {:description "The updated user."
                  :content {:application-json {:schema {:$ref "#/components/schemas/User"}}}}
           "400" {:description "The request to update the specified user was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def old-users-path
  {:get
   {:operation-id :index-old-users
    :summary "Return all users for the given OLD matching the supplied query."
    :description "Return all users with access to the given OLD and matching the supplied query and pagination parameters."
    :tags [:Users]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/pageQueryParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/itemsPerPageQueryParam"}]
    :responses
    (assoc common-path-responses
           "200" {:description "A page of users."
                  :content {:application-json {:schema {:$ref "#/components/schemas/PageOfUsers"}}}}
           "400" {:description "The request for users was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   #_#_:post
   {:operation-id :create-user
    :summary "Create a new user."
    :description "Create a new user then return the created user."
    :tags [:Users]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}]
    :request-body
    {:description "The payload to create a user. This payload must conform to the schema UserWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/UserWrite"}}}}
    :responses
    (assoc common-path-responses
           "201" {:description "The created user, including server-side-generated values such as the ID."
                  :content {:application-json {:schema {:$ref "#/components/schemas/User"}}}}
           "400" {:description "The request to create a new user was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def new-user-path
  {:get
   {:operation-id :new-user
    :summary "Return the data needed to create a new user."
    :description "Return the data needed to create a new user."
    :tags [:Users]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}]
    :responses
    (assoc common-path-responses
           "200" {:description "The data needed to create a new user."
                  :content {:application-json {:schema {:$ref "#/components/schemas/NewUserData"}}}})}})

(def edit-user-path
  {:get
   {:operation-id :edit-user
    :summary "Return the data needed to update an existing user."
    :description "Return the data needed to update an existing user."
    :tags [:Users]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/userIDParam"}]
    :responses
    (assoc common-path-responses
           "200" {:description "The data needed to update the specified user."
                  :content {:application-json {:schema {:$ref "#/components/schemas/EditUserData"}}}}
           "400" {:description "The request for the data needed to update the specified user was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def users-path
  {:get
   {:operation-id :index-users
    :summary "Return all users matching the supplied query."
    :description "Return all users matching the supplied query and pagination parameters."
    :tags [:Users]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/pageQueryParam"}
                 {:$ref "#/components/parameters/itemsPerPageQueryParam"}]
    :responses
    (assoc common-path-responses
           "200" {:description "A page of users."
                  :content {:application-json {:schema {:$ref "#/components/schemas/PageOfUsers"}}}}
           "400" {:description "The request for users was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :post
   {:operation-id :create-user
    :summary "Create a new user."
    :description "Create a new user then return the created user."
    :tags [:Users]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}]
    :request-body
    {:description "The payload to create a user. This payload must conform to the schema UserWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/UserWrite"}}}}
    :responses
    (assoc common-path-responses
           "201" {:description "The created user, including server-side-generated values such as the ID."
                  :content {:application-json {:schema {:$ref "#/components/schemas/User"}}}}
           "400" {:description "The request to create a new user was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def plan-path
  {:get
   {:operation-id :show-plan
    :summary "Return the plan with the provided ID."
    :description "Return the plan with the provided ID."
    :tags [:Plans]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/planIDParam"}]
    :responses
    (assoc common-path-responses
           "200" {:description "The plan."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Plan"}}}}
           "400" {:description "The request for a specific plan was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :delete
   {:operation-id :delete-plan
    :summary "Delete the plan with the provided ID."
    :description "Delete the plan with the provided ID. This is a soft delete. The plan data are not actually removed from the database. However, the system will behave as though the plan no longer exists."
    :tags [:Plans]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/planIDParam"}]
    :responses
    (assoc common-path-responses
           "200" {:description "The deleted plan."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Plan"}}}}
           "400" {:description "The request to delete the specified plan was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :put
   {:operation-id :update-plan
    :summary "Update the plan with the provided ID."
    :description "Update the plan with the provided ID using the JSON payload of the request."
    :tags [:Plans]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/planIDParam"}]
    :request-body
    {:description "The payload representing the desired new state of the plan. This payload must conform to the schema PlanWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/PlanWrite"}}}}
    :responses
    (assoc common-path-responses
           "200" {:description "The updated plan."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Plan"}}}}
           "400" {:description "The request to update the specified plan was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def plans-path
  {:post
   {:operation-id :create-plan
    :summary "Create a new plan."
    :description "Create a new plan then return the created plan."
    :tags [:Plans]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}]
    :request-body
    {:description "The payload to create a plan. This payload must conform to the schema PlanWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/PlanWrite"}}}}
    :responses
    (assoc common-path-responses
           "201" {:description "The created plan, including server-side-generated values such as the ID."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Plan"}}}}
           "400" {:description "The request to create a new plan was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def paths*
  ["/api/v1/login" login-path

   "/api/v1/{old_slug}/forms/new" new-form-path
   ;; "/api/v1/{old_slug}/forms/search" search-forms-path ;; TODO: operational? I think not ...
   "/api/v1/{old_slug}/forms/{form_id}/edit" edit-form-path
   "/api/v1/{old_slug}/forms/{form_id}" form-path
   "/api/v1/{old_slug}/forms" forms-path

   ;; "/api/v1/{old_slug}/users/new" new-old-user-path
   ;; "/api/v1/{old_slug}/users/{user_id}/edit" edit-old-user-path
   ;; "/api/v1/{old_slug}/users/{user_id}" old-user-path
   "/api/v1/{old_slug}/users" old-users-path

   "/api/v1/users/new" new-user-path
   "/api/v1/users/{user_id}/activate/{user_registration_key}" activate-user-path
   "/api/v1/users/{user_id}/edit" edit-user-path
   "/api/v1/users/{user_id}" user-path
   "/api/v1/users" users-path

   ;; "/api/v1/plans/{plan_id}" plan-path
   "/api/v1/plans" plans-path

   ])

(def paths
  (->> paths*
       (partition 2)
       (map vec)
       (into {})))

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
   :servers (mapv (fn [server] (dissoc server :id)) servers)
   :tags tags})
