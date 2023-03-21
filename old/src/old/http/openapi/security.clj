(ns old.http.openapi.security
  "The `run-security` fn implemented here implements the OpenAPI security
   check(s) declared via the OpenAPI spec. It takes an stateful `application`
   component containing the OpenAPI spec under `:spec` and a `ctx` map
   containing the validated Ring request under `:request`. It identifies the
   security strategy declared in the spec and runs the corresponding security
   handler, as defined under `http/security/`. At present, only the `:api-key`
   security strategy is supported. See
   https://swagger.io/docs/specification/authentication/."
  (:require [old.http.openapi.errors :as errors]
            [old.http.utils :as utils]))

(defn- locate-api-key-value
  "Locate the value of API Key by finding parameter `key-name` in the `in` of the
   supplied `request`."
  [key-name in request]
  (case in
    :header (get (utils/keywordize-keys (:headers request)) (utils/keyword-lower-case key-name))
    :query (get (utils/parse-query-string (:query-string request)) key-name)
    nil))

(defn- run-api-key-security
  "Locate the API key in the request and validate it using the `:api-key`
   security handler from the `application`. Look for the API key in the location
   specified by the `:in` of the scheme of the current OpenAPI security scheme.
   The value of the API key should be under the key specified by the `:name` of
   the security scheme schema. Return an updated `security-scheme` map with a
   new `:result` key whose value is whatever is returned by the security
   handler. The handler is expected to throw a response-encoding exception if
   the request is invalid."
  [application ctx security-scheme]
  (let [{:keys [security-handlers]} application
        api-key-handler (:api-key security-handlers)]
    (when-not api-key-handler
      (throw (errors/error-code->ex-info :error-unimplemented-security-handler)))
    (let [{in :in api-key-param :name} security-scheme]
      (if-let [api-key (locate-api-key-value api-key-param in (:request ctx))]
        (assoc security-scheme :result (api-key-handler application api-key))
        (throw (errors/error-code->ex-info :unauthenticated))))))

(defn- run-security-scheme
  "Run the provided `security-scheme`, if recognized."
  [application ctx security-scheme]
  (case (:type security-scheme)
    :api-key (run-api-key-security application ctx security-scheme)
    (throw (errors/error-code->ex-info :unauthenticated))))

(defn- run-security-schemes
  [application ctx securities]
  (->> securities
       (map (juxt key (comp (partial run-security-scheme application ctx) val)))
       (into {})))

(defn- root-securities
  "Using the OpenAPI security specification contained within `:spec` of the
   `ctx`, set `:securities` on said `ctx` to a map from security strategy keys
   to security scheme maps.
   Example `:security`: [{:api-key-auth []}]
   Example `:security-schemes`: {:api-key-auth {:type :api-key :in :header :name X-API-KEY}}
   Example `:securities`: {:api-key-auth {:type :api-key :in :header :name X-API-Key}}."
  [spec]
  (let [{:keys [security components]} spec
        {:keys [security-schemes]} components]
    (->> security
         (mapcat keys)
         set
         (map (fn [key] [key (key security-schemes)]))
         (into {}))))

(defn run-security
  "Run all security schemes entailed by the OpenAPI spec and associate the
   result under `:security` of the provided `ctx` map.

   NOTES:

   - Only the API key security type is supported here currently.
   - Only root-level securities are supported. Operation-specific securities are
     not supported.

   The `application` must contain a `:security-handlers` key whose value is a
   map from security types to handler functions that implement the security
   check. At present, the only security type recognized is `:api-key`. The value
   of this key must be a function that takes an application component and an API
   key value and returns a result if the key is valid or else throws a response
   exception.

   If the request does not contain the required data to perform any required
   authentications, then a 401 exception is thrown."
  [application ctx]
  (assoc ctx :security (run-security-schemes application ctx (root-securities (:spec application)))))
