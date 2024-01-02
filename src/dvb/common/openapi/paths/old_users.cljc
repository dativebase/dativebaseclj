(ns dvb.common.openapi.paths.old-users
  (:require [dvb.common.openapi.paths.common :as common]))

(def old-users-path
  {:get
   {:operation-id :index-old-users
    :summary "Return all users for the given OLD matching the supplied query."
    :description "Return all users with access to the given OLD and matching the supplied query and pagination parameters."
    :tags [:OLDUsers]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/pageQueryParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/itemsPerPageQueryParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "A page of users."
                  :content {:application-json {:schema {:$ref "#/components/schemas/PageOfUsers"}}}}
           "400" {:description "The request for users was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})
