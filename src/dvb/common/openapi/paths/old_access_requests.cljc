(ns dvb.common.openapi.paths.old-access-requests
  (:require [dvb.common.openapi.paths.common :as common]))

(def old-access-request-path
  {:get
   {:operation-id :show-old-access-request
    :summary "Return the OLD access request with the provided id."
    :description "Return the OLD access request with the provided id."
    :tags [:OLDAccessRequests]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/OLDAccessRequestIDParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The OLD access request."
                  :content {:application-json {:schema {:$ref "#/components/schemas/OLDAccessRequest"}}}}
           "400" {:description "The request for a specific OLD access request was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def old-access-requests-path
  {:post
   {:operation-id :create-old-access-request
    :summary "Create a new OLD access request."
    :description "Create a new OLD access request then return it."
    :tags [:OLDAccessRequests]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}]
    :request-body
    {:description "The payload to create an OLD access request. This payload must conform to the schema OLDAccessRequestWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/OLDAccessRequestWrite"}}}}
    :responses
    (assoc common/common-path-responses
           "201" {:description "The created OLD access request, including server-side-generated values such as the timestamp of creation."
                  :content {:application-json {:schema {:$ref "#/components/schemas/OLDAccessRequest"}}}}
           "400" {:description "The request to create a new OLD access request was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def requests-for-single-old-path
  {:get
   {:operation-id :access-requests-for-single-old
    :summary "Get all pending access requests for a target OLD."
    :description "Get all pending access requests for a target OLD."
    :tags [:OLDAccessRequests]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The collection of access requests targeting the specified OLD."
                  :content {:application-json {:schema {:$ref "#/components/schemas/OLDAccessRequests"}}}}
           "400" {:description "The request to fetch the access requests for a target OLD was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def approve-old-access-request-path
  {:put
   {:operation-id :approve-old-access-request
    :summary "Approve the OLD access request with the provided id."
    :description "Approve the OLD access request by transitioning its state to approved and making the referenced user a user of the specified OLD."
    :tags [:OLDAccessRequests]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/OLDAccessRequestIDParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The approved OLD access request."
                  :content {:application-json {:schema {:$ref "#/components/schemas/OLDAccessRequest"}}}}
           "400" {:description "The request to approve the specified OLD access request was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def reject-old-access-request-path
  {:put
   {:operation-id :reject-old-access-request
    :summary "Reject the OLD access request with the provided id."
    :description "Reject the OLD access request by transitioning its state to rejected."
    :tags [:OLDAccessRequests]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/OLDAccessRequestIDParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The rejected OLD access request."
                  :content {:application-json {:schema {:$ref "#/components/schemas/OLDAccessRequest"}}}}
           "400" {:description "The request to reject the specified OLD access request was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def retract-old-access-request-path
  {:put
   {:operation-id :retract-old-access-request
    :summary "Retract the OLD access request with the provided id."
    :description "Retract the OLD access request by transitioning its state to retracted."
    :tags [:OLDAccessRequests]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/OLDAccessRequestIDParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The retracted OLD access request."
                  :content {:application-json {:schema {:$ref "#/components/schemas/OLDAccessRequest"}}}}
           "400" {:description "The request to retract the specified OLD access request was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})
