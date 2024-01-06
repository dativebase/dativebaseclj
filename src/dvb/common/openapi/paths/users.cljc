(ns dvb.common.openapi.paths.users
  (:require [dvb.common.openapi.paths.common :as common]))

(def new-user-path
  {:get
   {:operation-id :new-user
    :summary "Return the data needed to create a new user."
    :description "Return the data needed to create a new user."
    :tags [:Users]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}]
    :responses
    (assoc common/common-path-responses
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
    (assoc common/common-path-responses
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
    (assoc common/common-path-responses
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
    :security [{}
               {:x-api-key []
                :x-app-id []}] ;; security is optional on purpose; creation of a (non-superuser) user does not require authentication
    :request-body
    {:description "The payload to create a user. This payload must conform to the schema UserWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/UserWrite"}}}}
    :responses
    (assoc common/common-path-responses
           "201" {:description "The created user, including server-side-generated values such as the ID."
                  :content {:application-json {:schema {:$ref "#/components/schemas/User"}}}}
           "400" {:description "The request to create a new user was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def user-path
  {:get
   {:operation-id :show-user
    :summary "Return the user with the provided ID."
    :description "Return the user with the provided ID."
    :tags [:Users]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/userIDParam"}
                 {:$ref "#/components/parameters/includePlansBooleanQueryParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The user."
                  :content {:application-json {:schema {:$ref "#/components/schemas/User"}}}}
           "400" {:description "The request for a specific user was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   ;; Deliberately disabled (perhaps provisionally)
   #_:delete
   #_{:operation-id :delete-user
      :summary "Delete the user with the provided ID."
      :description "Delete the user with the provided ID. This is a soft delete. The user data are not actually removed from the database. However, the system will behave as though the user no longer exists."
      :tags [:Users]
      :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                   {:$ref "#/components/parameters/userIDParam"}]
      :responses
      (assoc common/common-path-responses
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
    (assoc common/common-path-responses
           "200" {:description "The updated user."
                  :content {:application-json {:schema {:$ref "#/components/schemas/User"}}}}
           "400" {:description "The request to update the specified user was invalid."
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
    :security [] ;; no security on purpose; activation of a user does not require authentication
    :responses
    (assoc common/common-path-responses
           "200" {:description "Successful user activation request. The user has been fully created. The user's registration status has been changed from 'pending' to 'registered'."
                  :content {:application-json {:schema {:$ref "#/components/schemas/User"}}}}
           "400" {:description "The request to activate the user was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def user-plans-path
  {:get
   {:operation-id :user-plans
    :summary "Get the plans associated with the referenced user"
    :description "Get the plans associated with the referenced user"
    :tags [:Users]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/userIDParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The plans associated to the referenced user."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Plans"}}}}
           "400" {:description "The request for the plans of a user was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})
