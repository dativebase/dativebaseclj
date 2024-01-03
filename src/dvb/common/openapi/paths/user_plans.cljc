(ns dvb.common.openapi.paths.user-plans
  (:require [dvb.common.openapi.paths.common :as common]))

(def user-plans-path
  {:post
   {:operation-id :create-user-plan
    :summary "Create a new user plan. This represents a user's access to a plan."
    :description "Create a new user plan then return the created user plan."
    :tags [:UserPlans]
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

(def user-plan-path
  {:put
   {:operation-id :update-user-plan
    :summary "Update the user plan with the provided ID."
    :description "Update the user plan with the provided ID using the JSON payload of the request."
    :tags [:UserPlans]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/userPlanIDParam"}]
    :request-body
    {:description "The payload representing the desired new state of the user plan. This payload must conform to the schema UserPlanUpdate."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/UserPlanUpdate"}}}}
    :responses
    (assoc common/common-path-responses
           "200" {:description "The updated user plan."
                  :content {:application-json {:schema {:$ref "#/components/schemas/UserPlan"}}}}
           "400" {:description "The request to update the specified user plan was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :delete
   {:operation-id :delete-user-plan
    :summary "Delete the user plan with the provided ID."
    :description "Delete the user plan with the provided ID. This is a soft delete. The user plan data are not actually removed from the database. However, the system will behave as though the user plan relation no longer exists."
    :tags [:UserPlans]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/userPlanIDParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The deleted user plan."
                  :content {:application-json {:schema {:$ref "#/components/schemas/UserPlan"}}}}
           "400" {:description "The request to delete the specified user plan was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})
