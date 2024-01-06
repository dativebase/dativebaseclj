(ns dvb.common.openapi.paths.user-olds
  (:require [dvb.common.openapi.paths.common :as common]))

;; TODO UserOLDWrite
;; userOLDIDParam
;; UserOLDUpdate
;; UserOLD

(def user-olds-path
  {:post
   {:operation-id :create-user-old
    :summary "Create a new user OLD. This represents a user's access to an OLD."
    :description "Create a new user OLD then return the created user OLD."
    :tags [:UserOLDs]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}]
    :request-body
    {:description "The payload to create a user OLD. This payload must conform to the schema UserOLDWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/UserOLDWrite"}}}}
    :responses
    (assoc common/common-path-responses
           "201" {:description "The created user OLD, including server-side-generated values such as the ID."
                  :content {:application-json {:schema {:$ref "#/components/schemas/UserOLD"}}}}
           "400" {:description "The request to create a new user OLD was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def user-old-path
  {:put
   {:operation-id :update-user-old
    :summary "Update the user's role in an OLD by updating the user OLD with the provided ID."
    :description "Update the user OLD with the provided ID using the JSON payload of the request."
    :tags [:UserOLDs]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/userOLDIDParam"}]
    :request-body
    {:description "The payload representing the desired new state of the user OLD. This payload must conform to the schema UserOLDUpdate."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/UserOLDUpdate"}}}}
    :responses
    (assoc common/common-path-responses
           "200" {:description "The updated user OLD."
                  :content {:application-json {:schema {:$ref "#/components/schemas/UserOLD"}}}}
           "400" {:description "The request to update the specified user OLD was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :delete
   {:operation-id :delete-user-old
    :summary "Remove the user as a member of the OLD by deleting the user OLD with the provided ID."
    :description "Delete the user OLD with the provided ID. This is a soft delete. The user OLD data are not actually removed from the database. However, the system will behave as though the user OLD relation no longer exists."
    :tags [:UserOLDs]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/userOLDIDParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The deleted user OLD."
                  :content {:application-json {:schema {:$ref "#/components/schemas/UserOLD"}}}}
           "400" {:description "The request to delete the specified user OLD was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})
