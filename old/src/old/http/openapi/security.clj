(ns old.http.openapi.security
  "The `run-security` fn implemented here implements the OpenAPI security
   check(s) declared via the OpenAPI spec. It takes a stateful `http-component`
   component containing the OpenAPI spec under `:spec` and a `ctx` map
   containing the validated Ring request under `:request`. It identifies the
   security strategy declared in the spec and runs the corresponding security
   handler, as defined under `http/security/`. At present, only the `:api-key`
   security strategy is supported. See
   https://swagger.io/docs/specification/authentication/."
  (:require [clojure.string :as str]
            [old.http.openapi.errors :as errors]
            [old.http.utils :as utils]
            [taoensso.timbre :as log]))

(defn- locate-api-key-value
  "Locate the value of API Key by finding parameter `key-name` in the `in` of the
   supplied `request`."
  [key-name in request]
  (case in
    :header (get (utils/keywordize-keys (:headers request)) (utils/keyword-lower-case key-name))
    :query (get (utils/parse-query-string (:query-string request)) key-name)
    nil))

(defn- run-api-key-security
  "Locate the API key-related params in the request and validate them using the
   `:api-key`security handler from the `http-component`. Look for the API key in
   the locations specified by the `:in` values of the scheme of the current
   OpenAPI security scheme. The value of the API key-related params should be
   under the key specified by the `:name` of the security scheme schemas.
   Return an updated `security-scheme` map with a new `:result` key whose value
   is whatever is returned by the security handler. The handler is expected to
   throw a response-encoding exception if the request is invalid."
  [http-component ctx security-scheme-group]
  (let [{:keys [security-handlers]} http-component
        api-key-handler (:api-key security-handlers)]
    (when-not api-key-handler
      (throw (errors/error-code->ex-info :error-unimplemented-security-handler)))
    (let [api-key-data
          (->> security-scheme-group
               (map (fn [{in :in api-key-param :name}]
                      (if-let [val (locate-api-key-value api-key-param in (:request ctx))]
                        [(keyword (str/lower-case api-key-param)) val]
                        (do (log/warn "A required API key value was not provided in the request."
                                      {:name api-key-param
                                       :in in})
                            (throw (errors/error-code->ex-info :unauthenticated))))))
               (into {}))]
      (api-key-handler http-component ctx api-key-data))))

(defn- run-security-scheme-group
  "Run the provided `security-scheme-group`, if recognized."
  [http-component ctx security-scheme-group]
  (case (:type (first security-scheme-group))
    :api-key (run-api-key-security http-component ctx security-scheme-group)
    (do (log/warn "The implementation of this API does not recognize the specified security type."
                  {:type (:type (first security-scheme-group))})
        (throw (errors/error-code->ex-info :unauthenticated)))))

(defn- run-security-schemes
  [http-component ctx securities]
  (->> securities
       vals
       (group-by :type)
       (map (juxt key (comp (partial run-security-scheme-group http-component ctx) val)))
       (into {})))

(defn- root-securities
  "Using the OpenAPI security specification contained within `:spec` of the
   `ctx`, set `:securities` on said `ctx` to a map from security strategy keys
   to security scheme maps.
   Example `:security`: [{:api-key []}]
   Example `:security-schemes`: {:api-key {:type :api-key :in :header :name X-API-KEY}}
   Example `:securities`: {:api-key {:type :api-key :in :header :name X-API-Key}}."
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

   The `http-component` must contain a `:security-handlers` key whose value is a
   map from security types to handler functions that implement the security
   check. At present, the only security type recognized is `:api-key`. The value
   of this key must be a function that takes an HTTP component and an API
   key value and returns a result if the key is valid or else throws a response
   exception.

   If the request does not contain the required data to perform any required
   authentications, then a 401 exception is thrown."
  [http-component ctx]
  (assoc ctx :security (run-security-schemes
                        http-component
                        ctx
                        (root-securities (:spec http-component)))))
