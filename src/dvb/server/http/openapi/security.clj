(ns dvb.server.http.openapi.security
  "The `run-security` fn implemented here implements the OpenAPI security
   check(s) declared via the OpenAPI spec. It takes a stateful `http-component`
   component containing the OpenAPI spec under `:spec` and a `ctx` map
   containing the validated Ring request under `:request`. It identifies the
   security strategy declared in the spec and runs the corresponding security
   handler, as defined under `http/security/`. At present, only the `:api-key`
   security strategy is supported. See
   https://swagger.io/docs/specification/authentication/."
  (:require [clojure.string :as str]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.http.utils :as utils]
            [taoensso.timbre :as log]))

(defn- locate-api-key-value
  "Locate the value of API Key by finding parameter `key-name` in the `in` of the
   supplied `request`."
  [key-name in request]
  (case in
    :header (get (utils/lower-case-keywordize-keys (:headers request))
                 (utils/keyword-lower-case key-name))
    :query (get (utils/parse-query-string (:query-string request))
                key-name)
    :cookie (:cookies request)
    nil))

(defn- get-api-key-security-scheme-data
  "Locate the API key-related data in the request and return it as a map with a
   single key: the name of the OpenAPI security scheme as a Clojure keyword."
  [request {:as _security-scheme :keys [name in]}]
  (let [val (locate-api-key-value name in request)]
    (when-not val
      (log/warn "A required API key value was not provided in the request."
                {:name name
                 :in in})
      (throw (errors/error-code->ex-info :unauthenticated)))
    {(keyword (str/lower-case name)) val}))

(defn- get-security-scheme-data
  "Get the data from the request that matches the supplied OpenAPI
   `security-scheme`. Schemes are recognized by their `:type` value. Currently,
   the only value recognized is `:api-key`."
  [request security-scheme]
  (case (:type security-scheme)
    :api-key (get-api-key-security-scheme-data request security-scheme)
    (do (log/warn "The implementation of this API does not recognize the specified security type."
                  {:type type})
        (throw (errors/error-code->ex-info :error-unsupported-security-scheme)))))

(defn run-security-option
  "Run a single security option, i.e., one of the elements of `:security` of the
   OpenAPI specification of this API. The value of `security-option` is a vector
   of maps, each with a `:type` key."
  [http-component ctx security-option]
  (let [security-option-type (-> security-option first :type)
        security-handler (-> http-component :security-handlers
                             security-option-type)
        data (apply merge
                    (map (partial get-security-scheme-data (:request ctx))
                         security-option))]
    (when-not security-handler
      (throw (errors/error-code->ex-info :error-unimplemented-security-handler)))
    (security-handler http-component ctx data)))

(defn run-security
  "Run each security option entailed by the supplied OpenAPI spec under
   `:security`. As soon as one security option is successful, return the
   supplied `ctx` with a new `:security` key, whose value is the map returned by
   the first successful security handler; this map will have an
   `:authenticated?` key whose value is `true`.

   If no security options succeed, throw an exception that will result in a 401
   response.

   NOTES:

   - Only the `:api-key` security `:type` is supported currently.

   The `http-component` must contain a `:security-handlers` key whose value is a
   map from security types to handler functions that implement the appropriate
   security check. At present, the only security type recognized is `:api-key`."
  [{:as http-component
    {:as _spec :keys [components] global-security :security} :spec}
   {:as ctx {operation-security :security} :operation}]
  (let [security (or operation-security global-security)]
    (if (empty? security)
      ctx
      (let [security-schemes (:security-schemes components)
            {:as security-result :keys [authenticated? results]}
            (reduce
             (fn [agg security-option]
               (let [security-option (mapv security-schemes (keys security-option))
                     result (try (run-security-option http-component ctx
                                                      security-option)
                                 (catch Exception e {:authenticated? false
                                                     :exc e}))]
                 (if (:authenticated? result)
                   (reduced result)
                   (update agg :results conj result))))
             {:authenticated? false
              :results []}
             security)]
        (when-not authenticated?
          (when-let [exc (some-> results first :exc)]
            (throw exc))
          (log/warn "Authentication failed without an identifiable exception."
                    {:security-results results})
          (throw (errors/error-code->ex-info :unauthenticated)))
        (assoc ctx :security security-result)))))

(comment

  (require '[dvb.common.openapi.spec :as spec]
           '[dvb.common.openapi.serialize :as serialize])

  (def api (serialize/denormalize spec/api))

  (try (run-security {:spec api
                      :security-handlers
                      {:api-key
                       (fn [_system _ctx api-key-data]
                         {:authenticated? true
                          :data api-key-data})}}
                     {:request
                      {:headers
                       {"X-API-KEY" "dog123"
                        "X-APP-ID" "cat456"}}})
       (catch Exception e {:data (ex-data e)
                           :message (ex-message e)
                           :exc e}))

  {:request {:headers {"X-API-KEY" "dog123", "X-APP-ID" "cat456"}},
   :security
   {:authenticated? true, :data [{:x-api-key "dog123"} {:x-app-id "cat456"}]}}

)
