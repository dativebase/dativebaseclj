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
  (:require [dvb.common.openapi.parameters :as parameters]
            [dvb.common.openapi.paths.forms :as form-paths]
            [dvb.common.openapi.paths.login :as login-paths]
            [dvb.common.openapi.paths.old-users :as old-user-paths]
            [dvb.common.openapi.paths.plans :as plan-paths]
            [dvb.common.openapi.paths.users :as users-paths]
            [dvb.common.openapi.paths.user-plans :as user-plan-paths]
            [dvb.common.openapi.schemas :as schemas]))

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
   {:name :OLDUsers
    :description "Operations on users with access to a specified OLD."}
   {:name :Plans
    :description "Operations on plans."}
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

(def paths*
  ["/api/v1/login" login-paths/login-path
   "/api/v1/{old_slug}/forms/new" form-paths/new-form-path
   "/api/v1/{old_slug}/forms/{form_id}/edit" form-paths/edit-form-path
   "/api/v1/{old_slug}/forms/{form_id}" form-paths/form-path
   "/api/v1/{old_slug}/forms" form-paths/forms-path
   "/api/v1/{old_slug}/users" old-user-paths/old-users-path
   "/api/v1/users/new" users-paths/new-user-path
   "/api/v1/users/{user_id}/activate/{user_registration_key}" users-paths/activate-user-path
   "/api/v1/users/{user_id}/edit" users-paths/edit-user-path
   "/api/v1/users/{user_id}" users-paths/user-path
   "/api/v1/users" users-paths/users-path
   "/api/v1/plans/{plan_id}" plan-paths/plan-path
   "/api/v1/plans" plan-paths/plans-path
   "/api/v1/user-plans" user-plan-paths/user-plans-path])

(def paths
  (->> paths*
       (partition 2)
       (map vec)
       (into {})))

(def api
  {:components
   {:examples {}
    :parameters parameters/parameters
    :schemas schemas/schemas
    :security-schemes security-schemes}
   :info info
   :openapi "3.0.0"
   :paths paths
   :security security
   :servers (mapv (fn [server] (dissoc server :id)) servers)
   :tags tags})
