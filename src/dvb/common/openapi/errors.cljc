(ns dvb.common.openapi.errors
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
   :account-already-exists [400 "There is already an account in DativeBase with the supplied email."]
   :coercion-error [400 "Failed to coerce a value."]
   :coercion-to-boolean-error [400 "The provided string value cannot be coerced to a boolean."]
   :coercion-to-integer-error [400 "The provided string value cannot be coerced to a integer."]
   :coercion-to-number-error [400 "The provided string value cannot be coerced to a number."]
   :complex-validation-error [400 "The input is invalid according to a oneOf, anyOf or allOf validator."] ;; I'm not sure if this can actually ever be thrown
   :deletion-request-for-current-user [400 "The deletion request is prohibited because the authenticated user cannot delete itself."]
   :duplicate-items [400 "The provided array contains duplicate items. All items must be unique."]
   :inconsistent-pagination-inputs [400 "The provided pagination inputs are not consistent with the state of this API and the resource targeted."]
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
   :no-changes-in-update [400 "The update request failed because the provided changes would not actually update the referenced entity."]
   :object-missing-required-properties [400 "The provided object is missing at least one required key."]
   :old-slug-mismatch [400 "The OLD slug in the path and the OLD slug in the referenced entity do not match."]
   :one-of-validator-matches-multiple [400 "The input matches more than one oneOf validators."]
   :plan-with-olds-not-delible [400 "The attempt to delete a plan was halted because the plan is supporting active OLDs. First transfer the OLDs to a different plan, then delete the plan."]
   :required-parameter-absent [400 "A required parameter was not provided."]
   :required-json-request-body-absent [400 "A JSON request body is required for this operation."]
   :unique-email-constraint-violated [400 "There is already an entity in the system with the supplied email address."]
   :unique-slug-constraint-violated [400 "There is already an OLD in the system with the supplied slug."]
   :user-activation-failed [400 "The supplied user registration key was incorrect. The user could not be activated."]
   :users-olds-unique-constraint-violated [400 "There is already a user OLD relationship in the system linking the specified user and OLD. Update that one instead of creating a new one."]
   :users-plans-unique-constraint-violated [400 "There is already a user plan relationship in the system linking the specified user and plan. Update that one instead of creating a new one."]
   ;; 401
   :unauthenticated [401 "The supplied credentials were not valid. Authentication failed."]
   :unregistered-user [401 "The supplied credentials were valid but the user has not completed registration. The user must visit the email link that was sent to them in order to complete registration."]
   ;; 403
   :unauthorized [403 "The authenticated user is not authorized for the target operation."]
   ;; 404
   :entity-not-found [404 "The referenced entity could not be found. Please ensure that the supplied identifier is correct."]
   :unrecognized-operation [404 "The requested URL path and HTTP method combination is not recognized by this API."]
   :unrecognized-request-path [404 "The requested URL path is not recognized by this API."]
   :unrecognized-request-url [404 "The requested URL is not recognized by this API."]
   ;; 500
   :error-recognizing-request-url [500 "Error when attempting to validate the request URL."]
   :error-response-should-be-empty [500 "Error: the response should have an empty response body; however, the server was attempting to return a non-empty body."]
   :error-unimplemented-operation [500 "Error: server lacks an implementation for the requested operation."]
   :error-unimplemented-security-handler [500 "Error: server lacks an implementation for a required component of the request."]
   :error-unsupported-security-scheme [500 "Error: server lacks an implementation for a security scheme required in order to process the request."]
   :error-unrecognized-response-status [500 "Error: server attempted to return a response status that is inconsistent with this API."]
   :entity-creation-internal-error [500 "An unexpected exception occurred when attempting to create an entity."]
   :entity-deletion-internal-error [500 "An unexpected exception occurred when attempting to delete an entity."]
   :entity-update-internal-error [500 "An unexpected exception occurred when attempting to update an entity."]
   :logout-internal-error [500 "An unexpected exception occurred when attempting to logout."]
   :unexpected-error [500 "An unexpected error occurred while attempting to respond to the request."]
   :unrecognized-data-type-in-openapi-schema [500 "Unrecognized data type. Unable to validate."]})

(defn error-code->message [error-code]
  (let [[_status message] (or (error-code errors-map)
                              (:unexpected-error errors-map))]
    message))

(defn error-code->response
  ([error-code] (error-code->response error-code nil))
  ([error-code data]
   (let [[status message] (or (error-code errors-map)
                              (:unexpected-error errors-map))]
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
