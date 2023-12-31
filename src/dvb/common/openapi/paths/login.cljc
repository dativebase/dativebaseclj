(ns dvb.common.openapi.paths.login
  (:require [dvb.common.openapi.paths.common :as common]))

(def login-path
  {:post
   {:operation-id :login
    :summary "Login and create a temporary API key"
    :description "Login and create a temporary API key"
    :tags [:Authentication]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}]
    :security [] ;; no security on purpose
    :request-body
    {:description "The payload to login. This payload must conform to the schema Login."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/Login"}}}}
    :responses
    (assoc common/common-path-responses
           "200" {:description "Successful login request. The authenticated user and a newly-generated API key are both returned in the response. Subsequent requests to other endpoints must supply a valid API ID and key in the request in order to authenticate."
                  :content {:application-json {:schema {:$ref "#/components/schemas/UserAndAPIKey"}}}}
           "400" {:description "The request to login was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})
