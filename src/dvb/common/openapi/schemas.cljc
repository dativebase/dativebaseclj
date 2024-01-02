(ns dvb.common.openapi.schemas
  (:require [dvb.common.openapi.schemas.api-key :as api-key]
            [dvb.common.openapi.schemas.error :as error]
            [dvb.common.openapi.schemas.form :as form]
            [dvb.common.openapi.schemas.login :as login]
            [dvb.common.openapi.schemas.plan :as plan]
            [dvb.common.openapi.schemas.user :as user]
            [dvb.common.openapi.schemas.user-plan :as user-plan]))

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
   :FormWrite form/form-write
   :Login login/login
   :NewFormData form/new-form-data
   :NewUserData user/new-user-data
   :PageOfForms form/page-of-forms
   :PageOfUsers user/page-of-users
   :Plan plan/plan
   :PlanOfUser plan/plan-of-user
   :Plans plan/plans
   :PlanWrite plan/plan-write
   :User user/user
   :UserAndAPIKey user-and-api-key
   :UserPlan user-plan/user-plan
   :UserPlanWrite user-plan/user-plan-write
   :UserUpdate user/user-update
   :UserWrite user/user-write})
