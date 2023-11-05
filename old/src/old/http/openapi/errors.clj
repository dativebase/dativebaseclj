(ns old.http.openapi.errors
  "Functionality to make it easy to construct an `ex-info` using a single
   keyword which results in an exception whose `ex-data` map fully specifies an
   HTTP response. Example usage:
     => (ex-data (error-code->ex-info :unauthenticated))
     {:status 401
      :headers {Content-Type application/json}
      :body {:message Failure to authenticate.
             :error-code unauthenticated}}")

(def errors-map
  {;; 400
   :coercion-error [400 "Failed to coerce a value."]
   :coercion-to-boolean-error [400 "The provided string value cannot be coerced to a boolean."]
   :coercion-to-integer-error [400 "The provided string value cannot be coerced to a integer."]
   :coercion-to-number-error [400 "The provided string value cannot be coerced to a number."]
   :complex-validation-error [400 "The input is invalid according to a oneOf, anyOf or allOf validator."] ;; I'm not sure if this can actually ever be thrown
   :duplicate-items [400 "The provided array contains duplicate items. All items must be unique."]
   :inconsistent-pagination-inputs [400 "The provided pagination inputs are not consistent with the state of this OLD and the resource targeted."]
   :invalid-array [400 "The provided value is not a valid array."]
   :invalid-boolean [400 "The provided value is not a valid boolean."]
   :invalid-date [400 "The provided value is not a valid date string as per RFC 3339, section 5.6."]
   :invalid-date-time [400 "The provided value is not a valid date-time string as per RFC 3339, section 5.6."]
   :invalid-given-enum [400 "The provided value is not one of the allowed values, given the enumeration in the spec."]
   :invalid-given-pattern [400 "The provided value does not match the prescribed pattern (regular expression)."]
   :invalid-integer [400 "The provided value is not a valid integer."]
   :invalid-json-request-body [400 "The JSON request body is not valid."]
   :invalid-number [400 "The provided value is not a valid number."]
   :invalid-object [400 "The provided value is not a valid object."]
   :invalid-request-params [400 "The request params from either the query string or the path were invalid."]
   :invalid-string [400 "The provided value is not a valid string."]
   :invalid-url [400 "The provided value is not a valid URL."]
   :invalid-uuid [400 "The provided value is not a valid UUID string."]
   :malformed-json-in-request-body [400 "Malformed JSON in request body."]
   :maximum-violation [400 "The provided value is greater than the maximum allowed value."]
   :max-items-violation [400 "The array value contains more than the maximum allowed number of items."]
   :minimum-violation [400 "The provided value is less than the minimum allowed value."]
   :min-items-violation [400 "The array value contains fewer than the minimum allowed number of items."]
   :object-missing-required-properties [400 "The provided object is missing at least one required key."]
   :one-of-validator-matches-multiple [400 "The input matches more than one oneOf validators."]
   :required-parameter-absent [400 "A required parameter was not provided."]
   :required-json-request-body-absent [400 "A JSON request body is required for this operation."]
   ;; 401
   :unauthenticated [401 "Failure to authenticate."]
   ;; 403
   :unauthorized [403 "The authenticated user is not authorized for the target operation."]
   ;; 404
   :unrecognized-operation [404 "The requested URL path and HTTP method combination is not recognized by this API."]
   :unrecognized-request-path [404 "The requested URL path is not recognized by this API."]
   :unrecognized-request-url [404 "The requested URL is not recognized by this API."]
   ;; 500
   :error-recognizing-request-url [500 "Error when attempting to validate the request URL."]
   :error-response-should-be-empty [500 "Error: the response should have an empty response body; however, the server was attempting to return a non-empty body."]
   :error-unimplemented-operation [500 "Error: server lacks an implementation for the requested operation."]
   :error-unimplemented-security-handler [500 "Error: server lacks an implementation for a required component of the request."]
   :error-unrecognized-response-status [500 "Error: server attempted to return a response status that is inconsistent with this API."]
   :form-creation-internal-error [500 "An unexpected exception occurred when attempting to create a form."]
   :old-unexpected-error [500 "An unexpected error occurred while attempting to respond to the request."]
   :unrecognized-data-type-in-openapi-schema [500 "Unrecognized data type. Unable to validate."]})

(defn error-code->message [error-code]
  (let [[_status message] (or (error-code errors-map)
                              (:old-unexpected-error errors-map))]
    message))

(defn error-code->response
  ([error-code] (error-code->response error-code nil))
  ([error-code data]
   (let [[status message] (or (error-code errors-map)
                              (:old-unexpected-error errors-map))]
     {:status status
      :headers {"Content-Type" "application/json"}
      :body {:errors [(cond-> {:message message
                                 :error-code (name error-code)}
                          data
                          (assoc :data data))]}})))

(defn error-code->ex-info
  ([error-code] (error-code->ex-info error-code nil))
  ([error-code data] (error-code->ex-info error-code data nil))
  ([error-code data exception]
   (ex-info (error-code->message error-code)
            (error-code->response error-code data)
            exception)))
