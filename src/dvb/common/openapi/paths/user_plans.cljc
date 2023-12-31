(ns dvb.common.openapi.paths.user-plans
  (:require [dvb.common.openapi.paths.common :as common]))

(def user-plans-path
  {:post
   {:operation-id :create-user-plan
    :summary "Create a new user plan. This represents a user's access to a plan."
    :description "Create a new user plan then return the created user plan."
    :tags [:Plans]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}]
    :request-body
    {:description "The payload to create a user plan. This payload must conform to the schema UserPlanWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/UserPlanWrite"}}}}
    :responses
    (assoc common/common-path-responses
           "201" {:description "The created user plan, including server-side-generated values such as the ID."
                  :content {:application-json {:schema {:$ref "#/components/schemas/UserPlan"}}}}
           "400" {:description "The request to create a new plan was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})
