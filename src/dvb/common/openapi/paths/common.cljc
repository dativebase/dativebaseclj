(ns dvb.common.openapi.paths.common)

(def common-path-responses
  {"401" {:description "The client is not authenticated and therefore cannot perform this operation."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorUnauthorized401"}}}}
   "403" {:description "The client is not authorized to perform this operation."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorUnauthorized403"}}}}
   "404" {:description "The operation or endpoint of the request was not found. The server does not recognize the path of the requests or the path is recognized but the method is not."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorNotFound404"}}}}
   "406" {:description "Either no `Accept` header was provided or the provided header is not recognized. The only currently recognized accept header is `application/json`."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorUnrecognizedAcceptHeader406"}}}}
   "429" {:description "Too many requests. This client has exceeded this API's request rate limit. The client may retry after the period specified in the response and the X header."
          :headers {:Retry-After {:description "The number of seconds that the client must wait before making further requests to this API."
                                  :schema {:type :integer
                                           :minimum 0}
                                  :example 60}}
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorTooManyRequests429"}}}}
   "500" {:description "There was an error in DativeBase while attempting to respond to the request. The operators of this service will be alerted to the issue and will address it in a timely manner."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorServer500"}}}}
   "503" {:description "The DativeBase is unavailable."
          :content {:application-json {:schema {:$ref "#/components/schemas/ErrorUnavailable503"}}}}})

