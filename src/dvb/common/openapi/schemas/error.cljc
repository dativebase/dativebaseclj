(ns dvb.common.openapi.schemas.error
  "Error Response Specs:

  - Error objects:
    - `Error`
    - `ErrorUnauthorized`
    - `ErrorNotFound`
    - `ErrorUnrecognizedAcceptHeader`
    - `ErrorTooManyRequests`
    - `ErrorServer`
    - `ErrorUnavailable`
  - Error responses (containing one or more error objects)
    - `ErrorBadRequest400`
    - `ErrorUnauthorized401`
    - `ErrorUnauthorized403`
    - `ErrorNotFound404`
    - `ErrorUnrecognizedAcceptHeader406`
    - `ErrorTooManyRequests429`
    - `ErrorServer500`
    - `ErrorUnavailable503`

   All error objects look like:
     {:message A valid string is required.
      :error-code invalid-string}
   All error responses look like:
     {:errors [{:message A valid string is required.
                :error-code invalid-string}]}"
  (:require [dvb.common.utils :as u]))

;; Error Objects

;; `Error`
(def error
  {:type :object
   :description "An error object for representing an arbitrary failure."
   :properties {:message {:type :string
                          :description "A message describing the nature of the error. This should be written for human consumption and possible display to users."
                          :example "A valid string is required."}
                :error-code {:type :string
                             :description "A short error code for uniquely identifying and categorizing the failure."
                             :example "invalid-string"}
                :data {:type :object
                       :description "An optional object. This may include data that are relevant to understanding the error."}}
   :required [:message
              :error-code]
   :example {:message "A valid string is required."
             :error-code "invalid-string"
             :data {:some "optional data structure"}}})

;; `ErrorUnauthorized`
(def error-unauthorized
  (let [example-message "The client is not authenticated. Therefore, the client is not authorized to make this request."]
    (-> error
        (assoc :description "An error object for indicating that the client has failed to authenticate and is therefore prohibited from making this request.")
        (assoc-in [:properties :message :example] example-message)
        (assoc-in [:properties :error-code]
                  {:type :string
                   :enum ["deactivated-user"
                          "unauthorized"
                          "unauthenticated"
                          "unregistered-user"]
                   :description "A short error code for uniquely identifying and categorizing the failure."
                   :example "unauthorized"})
        (assoc :example {:message example-message
                         :error-code "unauthorized"}))))

;; `ErrorNotFound`
(def error-not-found
  (let [example-message "The referenced entity could not be found. Please ensure that the supplied identifier is correct."]
    (-> error
        (assoc :description "An error object for indicating that the client has failed to authenticate and is therefore prohibited from making this request.")
        (assoc-in [:properties :message :example] example-message)
        (assoc-in [:properties :error-code] {:type :string
                                             :enum ["entity-not-found"
                                                    "unrecognized-operation"
                                                    "unrecognized-request-path"
                                                    "unrecognized-request-url"]
                                             :description "A short error code for uniquely identifying and categorizing the failure."
                                             :example "entity-not-found"})
        (assoc :example {:message example-message
                         :error-code "entity-not-found"}))))

;; `ErrorUnrecognizedAcceptHeader`
(def error-unrecognized-accept-header
  (let [example-message "Either no `Accept` header was provided or the provided header is not recognized. The only currently recognized accept header is `application/json`."]
    (-> error
        (assoc :description "An error object for indicating that the client has failed to provide a recognized `Accept` header in the request.")
        (assoc-in [:properties :message :example] example-message)
        (assoc-in [:properties :error-code] {:type :string
                                             :enum ["unrecognized-accept-header"]
                                             :description "A short error code for uniquely identifying and categorizing the failure."
                                             :example "unrecognized-accept-header"})
        (assoc :example {:message example-message
                         :error-code "unrecognized-accept-header"}))))

;; `ErrorTooManyRequests`
(def error-too-many-requests
  (let [retry-after 60
        example-message (u/format "This client has made too many requests to this service. Please wait for %s seconds before issuing any further requests." retry-after)]
    {:type :object
     :description "An error object for representing a 'Too Many Requests' response."
     :properties {:message {:type :string
                            :description "A message describing the nature of the error. This should be written for human consumption and possible display to users."
                            :example example-message}
                  :error-code {:type :string
                               :enum ["too-many-requests"]
                               :description "A short error code for uniquely identifying and categorizing the failure."
                               :example "too-many-requests"}
                  :retry-after {:type :integer
                                :minimum 0
                                :description "The delay in seconds. A non-negative integer indicating the seconds to delay after the response is received."
                                :example retry-after}}
     :required [:message
                :error-code
                :retry-after]
     :example {:message example-message
               :error-code "too-many-requests"
               :retry-after retry-after}}))

;; `ErrorServer`
(def error-server
  (let [example-message "An unexpected error occurred while attempting to respond to the request."]
    (-> error
        (assoc :description "This JSON response body describes an error, on the side of the server, encountered while attempting to respond to the request.")
        (assoc-in [:properties :message :example] example-message)
        (assoc-in [:properties :error-code :example] "unexpected-error")
        (assoc :example {:message example-message
                         :error-code "unexpected-error"}))))

;; `ErrorUnavailable`
(def error-unavailable
  (let [example-message "The server is unavailable."]
    (-> error
        (assoc :description "This JSON response body describes an error where the web server is unavailable.")
        (assoc-in [:properties :message :example] example-message)
        (assoc-in [:properties :error-code :example] "error-unavailable")
        (assoc :example {:message example-message
                         :error-code "error-unavailable"}))))

;; Error Responses (containing one or more error objects)

;; `ErrorBadRequest400`
(def error-bad-request-400
  {:type :object
   :description "An error object for representing a 'Bad Request' response."
   :properties {:errors {:type :array
                         :description "The errors expressing why the request was bad."
                         :min-items 1
                         :items {:$ref "#/components/schemas/Error"}}}
   :required [:errors]
   :example {:errors [{:message "A valid string is required."
                       :error-code "invalid-string"}]}})

;; `ErrorUnauthorized401`
(def error-unauthorized-401
  (-> error-bad-request-400
      (assoc :description "An error object for indicating that the client has failed to authenticate and is therefore prohibited from making this request.")
      (assoc-in [:properties :errors :description] "Errors expressing details of the failure to authenticate.")
      (assoc-in [:properties :errors :items] {:$ref "#/components/schemas/ErrorUnauthorized"})
      (assoc :example {:errors [(:example error-unauthorized)]})))

;; `ErrorUnauthorized403`
(def error-unauthorized-403
  (-> error-bad-request-400
      (assoc :description "An error object for indicating that the authenticated client lacks the role required in order to access the target resource in the target OLD.")
      (assoc-in [:properties :errors :description] "Errors expressing details of the failure to authorize.")
      (assoc-in [:properties :errors :items] {:$ref "#/components/schemas/ErrorUnauthorized"})
      (assoc :example {:errors [{:message "The client is not authorized to execute the target operation in the referenced OLD."
                                 :error-code "unauthorized"}]})))

;; `:ErrorNotFound404`
(def error-not-found-404
  (-> error-bad-request-400
      (assoc :description "An error object for indicating that the endpoint or operation was not found.")
      (assoc-in [:properties :errors :description] "Errors expressing details about the requests for which a handler could not be found.")
      (assoc-in [:properties :errors :items] {:$ref "#/components/schemas/ErrorNotFound"})
      (assoc :example {:errors [{:message "The referenced entity could not be found. Please ensure that the supplied identifier is correct."
                                 :error-code "entity-not-found"}]})))

;; `ErrorUnrecognizedAcceptHeader406`
(def error-unrecognized-accept-header-406
  (-> error-bad-request-400
      (assoc :description "An error object for indicating that the client has failed to provide a recognized `Accept` header in the request.")
      (assoc-in [:properties :errors :description] "Errors expressing details of the failure to provide a recognized `Accept` header.")
      (assoc-in [:properties :errors :items] {:$ref "#/components/schemas/ErrorUnrecognizedAcceptHeader"})
      (assoc :example {:errors [(:example error-unrecognized-accept-header)]})))

;; `ErrorTooManyRequests429`
(def error-too-many-requests-429
  (-> error-bad-request-400
      (assoc :description "An error object for representing a 'Too Many Requests' response.")
      (assoc-in [:properties :errors :description] "Errors expressing details around issuing too many requests.")
      (assoc-in [:properties :errors :items] {:$ref "#/components/schemas/ErrorTooManyRequests"})
      (assoc :example {:errors [(:example error-too-many-requests)]})))

;; `ErrorServer500`
(def error-server-500
  (-> error-bad-request-400
      (assoc :description "This JSON response body describes an error, on the side of the server, encountered while attempting to respond to the request.")
      (assoc-in [:properties :errors :items] {:$ref "#/components/schemas/ErrorServer"})
      (assoc :example {:errors [(:example error-server)]})))

;; `ErrorUnavailable503`
(def error-unavailable-503
  (-> error-bad-request-400
      (assoc :description "This JSON response body describes an error where the web server is not available.")
      (assoc-in [:properties :errors :items] {:$ref "#/components/schemas/ErrorUnavailable"})
      (assoc :example {:errors [(:example error-unavailable)]})))
