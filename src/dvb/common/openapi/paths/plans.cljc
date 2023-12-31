(ns dvb.common.openapi.paths.plans
  (:require [dvb.common.openapi.paths.common :as common]))

(def plan-path
  {:get
   {:operation-id :show-plan
    :summary "Return the plan with the provided ID."
    :description "Return the plan with the provided ID."
    :tags [:Plans]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/planIDParam"}]
    :responses
    (assoc common/common-path-responses
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
    (assoc common/common-path-responses
           "200" {:description "The deleted plan."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Plan"}}}}
           "400" {:description "The request to delete the specified plan was invalid."
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
    (assoc common/common-path-responses
           "201" {:description "The created plan, including server-side-generated values such as the ID."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Plan"}}}}
           "400" {:description "The request to create a new plan was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})
