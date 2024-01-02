(ns dvb.common.openapi.paths.olds
  (:require [dvb.common.openapi.paths.common :as common]))

(def old-path
  {:get
   {:operation-id :show-old
    :summary "Return the OLD with the provided slug."
    :description "Return the OLD with the provided slug."
    :tags [:OLDs]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The OLD."
                  :content {:application-json {:schema {:$ref "#/components/schemas/OLD"}}}}
           "400" {:description "The request for a specific OLD was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :put
   {:operation-id :update-old
    :summary "Update the OLD with the provided slug."
    :description "Update the OLD with the provided slug using the JSON payload of the request."
    :tags [:OLDs]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :request-body
    {:description "The payload representing the desired new state of the OLD. This payload must conform to the schema OLDUpdate."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/OLDUpdate"}}}}
    :responses
    (assoc common/common-path-responses
           "200" {:description "The updated OLD."
                  :content {:application-json {:schema {:$ref "#/components/schemas/OLD"}}}}
           "400" {:description "The request to update the specified OLD was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :delete
   {:operation-id :delete-old
    :summary "Delete the OLD with the provided slug."
    :description "Delete the OLD with the provided slug. This is a soft delete. The OLD data are not actually removed from the database. However, the system will behave as though the OLD no longer exists."
    :tags [:OLDs]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The deleted OLD."
                  :content {:application-json {:schema {:$ref "#/components/schemas/OLD"}}}}
           "400" {:description "The request to delete the specified OLD was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def olds-path
  {:get
   {:operation-id :index-olds
    :summary "Return all OLDs matching the supplied query."
    :description "Return all OLDs matching the supplied query and pagination parameters."
    :tags [:OLDs]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/pageQueryParam"}
                 {:$ref "#/components/parameters/itemsPerPageQueryParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "A page of OLDs."
                  :content {:application-json {:schema {:$ref "#/components/schemas/PageOfOLDs"}}}}
           "400" {:description "The request for OLDs was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :post
   {:operation-id :create-old
    :summary "Create a new OLD."
    :description "Create a new OLD then return the created OLD."
    :tags [:OLDs]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}]
    :request-body
    {:description "The payload to create a OLD. This payload must conform to the schema OLDWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/OLDWrite"}}}}
    :responses
    (assoc common/common-path-responses
           "201" {:description "The created OLD, including server-side-generated values such as the timestamp of creation."
                  :content {:application-json {:schema {:$ref "#/components/schemas/OLD"}}}}
           "400" {:description "The request to create a new OLD was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})
