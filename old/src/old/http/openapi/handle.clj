(ns old.http.openapi.handle
  "The `handle` fn provided by this ns takes a stateful `application` component
   and a Ring `request` map and returns a Ring response map. This function
   implements HTTP (de)serialization, routing, security checking and
   input/output validation all based on the OpenAPI spec defined in
   `http.openapi.spec/api`."
  (:require [clojure.string :as str]
            [old.http.openapi.errors :as errors]
            [old.http.openapi.security :as security]
            [old.http.openapi.validate :as validate]
            [old.http.utils :as utils]
            [reitit.core :as reitit]))

;; Helper functions

(defn- get-request-url
  "Reconstruct the requested URL from the parts of the Ring request map."
  [{:keys [scheme server-name server-port uri]}]
  (str (name scheme)
       "://"
       server-name
       (when server-port (str ":" server-port))
       uri))

(defn- get-spec-server
  "Return the first server in the OpenAPI `spec` that is a prefix of
   `request-url`."
  [request-url spec]
  (->> spec
       :servers
       (map :url)
       (filter #(str/starts-with? request-url %))
       first))

;; Handle sub-step functions

(defn- construct-router
  "Construct a reitit router based on the coll of `paths` in the OpenAPI spec.
   Assoc it under `:router`."
  [{:keys [spec] :as ctx}]
  (assoc ctx :router (->> spec :paths keys (map vector) reitit/router)))

(defn- recognize-request-url
  "Validate that the request URL is recognized by the OpenAPI spec. If it
   is, set `request-path`, `spec-server` and `request-url` on the context."
  [{:keys [request spec] :as ctx}]
  (try (let [request-url (get-request-url request)
             spec-server (get-spec-server request-url spec)]
         (if spec-server
           (assoc ctx
                  :request-path (:uri request)
                  :spec-server spec-server
                  :request-url request-url)
           (throw (errors/error-code->ex-info :unrecognized-request-url))))
       (catch clojure.lang.ExceptionInfo e (throw e))
       (catch Exception e
         (throw (errors/error-code->ex-info :error-recognizing-request-url nil e)))))

(defn- recognize-request-path
  "Validate that the request path is recognized by the OpenAPI spec. If it is,
   then set `route-match` to a reitit route match map."
  [{:keys [router request-path] :as ctx}]
  (if-let [route-match (reitit/match-by-path router request-path)]
    (assoc ctx :route-match route-match)
    (throw (errors/error-code->ex-info :unrecognized-request-path))))

(defn- recognize-request-operation
  "Validate that the request operation (path plus HTTP method) is recognized by
   the OpenAPI spec. If it is, then set the operation as data (from the OpenAPI
   spec) under `:operation`."
  [{{method :request-method} :request
    {paths :paths} :spec
    {template :template} :route-match
    :as ctx}]
  (if-let [operation (get-in paths [template method])]
    (assoc ctx :operation operation)
    (throw (errors/error-code->ex-info :unrecognized-operation))))

(defn- validate-request-path
  "Parse and then validate the path params from the request against the
   path-specific OpenAPI parameter specs for the operation identified with this
   request. If valid, set the validated (and possibly type-coerced) path params
   map under `:path`. If invalid, throw a 400 exception."
  [{{param-specs :parameters} :operation
    {path-params :path-params} :route-match :as ctx}]
  (reduce
   (fn [ctx* param-spec]
     (utils/deep-merge ctx* {:path
                             (validate/validate-parameters-against-single-spec
                              path-params param-spec)}))
   ctx
   (for [{:as spec :keys [in]} param-specs :when (= :path in)] spec)))

(defn- validate-request-query
  "Parse and then validate the query string params from the request against
   the query string-specific OpenAPI parameter specs for the operation
   identified with this request. If valid, set the validated (and possibly
   type-coerced) query string params map under `:query`. If invalid, throw a 400
   exception."
  [{{param-specs :parameters} :operation
    {:keys [query-string]} :request :as ctx}]
  (let [query-string-map (utils/parse-query-string query-string)]
    (reduce
     (fn [ctx* param-spec]
       (utils/deep-merge ctx* {:query
                               (validate/validate-parameters-against-single-spec
                                query-string-map param-spec)}))
     ctx
     (for [{:as spec :keys [in]} param-specs :when (= :query in)] spec))))

(defn- validate-request-headers
  "Validate the request headers according to the OpenAPI spec for the
   operation identified with this request. If valid, set the headers map under
   `:headers`. If invalid, throw a 400 exception."
  [{{param-specs :parameters} :operation
    {:keys [headers]} :request :as ctx}]
  (let [headers (into {} (for [[k v] headers] [(keyword k) v]))]
    (reduce
     (fn [acc param-spec]
       (utils/deep-merge acc {:headers (validate/validate-parameters-against-single-spec
                                        headers param-spec)}))
     ctx
     (for [{:as spec :keys [in]} param-specs :when (= :header in)] spec))))

(defn- validate-request-body
  "Validate the JSON request body according to the OpenAPI spec for the
   operation identified with this request. If valid, set the body under
   `:request-body`."
  [{{{{{:keys [schema]} :application-json} :content
      :keys [required]} :request-body} :operation
    {actual-request-body :body} :request
    :as ctx}]
  (cond
    (not schema) ;; an absent schema should indicate a malformed OpenAPI spec, no?
    ctx
    (and required (not actual-request-body))
    (throw (errors/error-code->ex-info :required-json-request-body-absent))
    :else
    (assoc ctx :request-body (validate/validate actual-request-body schema "RequestBody"))))

(defn- run-operation
  [application {{:keys [operation-id]} :operation :as ctx}]
  (if-let [operation-fn (-> application :operations operation-id)]
    (operation-fn application ctx)
    (throw (errors/error-code->ex-info :error-unimplemented-operation {:operation-id operation-id}))))

(defn- set-response-validator
  "Set `:response-validator` on `ctx` to a function that takes a response for
  the current operation and throws an exception if that response is invalid.
  This response validator must be called from the code of the operation function
  itself. This is because we want failure to construct a valid response to be
  able to roll back any state changes performed in the context of the operation."
  [{{:keys [responses operation-id]} :operation :as ctx}]
  (assoc
   ctx
   :response-validator
   ;; TODO: validate response headers?
   (fn [{:as response :keys [status body]}]
     (let [response-spec (get responses (str status))
           content (:content response-spec)
           schema (-> content :application-json :schema)]
       (cond (not response-spec)
             (throw (errors/error-code->ex-info
                     :error-unrecognized-response-status
                     {:operation-id operation-id
                      :unrecognized-status status}))
             (and (not content) response)
             (throw (errors/error-code->ex-info
                     :error-response-should-be-empty
                     {:operation-id operation-id}))
             :else
             (assoc response :body (validate/validate body schema "ResponseBody")))))))

(defn validate-request
  "Validate the request according to its recognized OpenAPI operation.
   Take as input the context map returned by `recognize-operation`, which
   contains the current operation and the raw request. If the request is valid,
   add the following keys to the context:
   - `:headers`: validated headers map, with possible type coercion of values as per spec
   - `:path`: validated path parameters map, with possible type coercion of values as per spec
   - `:query`: validated query string parameters map, with possible type coercion of values as per spec
   - `:request-body`: validated request body, from JSON deserialized
   If the request is invalid, throw an `ExceptionInfo` whose `ex-data` contains
   the data need to construct the appropriate HTTP response. All error responses
   thrown here should be valid according to `:responses` of the current
   operation of the OpenAPI spec."
  [ctx]
  (->> ctx
       validate-request-headers
       validate-request-path
       validate-request-query
       validate-request-body))

(defn recognize-operation
  "Recognize the OpenAPI 'operation' (= requesth path + HTTP method) of this
   request. Take a request map and an OpenAPI spec map and return a new context
   map. If the request method and path constitute an operation recognized by this
   API, then return the context map with the following keys:
   - `:route-match`: map with keys `:template` and `:path-params`
   - `:operation`: the operation map, from the OpenAPI spec, with keys `:operation-id`, etc.
   - `:response-validator`: a function that can validate a response wrt the recognized operation.
   If the request does not constitute a recognized operation, throw an
   `ExceptionInfo` whose `ex-data` contains the data need to construct the
   appropriate 404 or 500 HTTP response. Possible error codes thrown:
   - `:unrecognized-request-url`: 404
   - `:unrecognized-request-path`: 404
   - `:unrecognized-operation`: 404
   - `:error-recognizing-request-url`: 500
   Note that the above errors are not validated according to the OpenAPI spec
   because operation recogniztion is required in order to identify the
   appropriate response schema for validation."
  [request spec]
  (->> {:request request :spec spec}
       construct-router
       recognize-request-url
       recognize-request-path
       recognize-request-operation
       set-response-validator))

(defn exception->response
  "Given an `exception`, attempt to extract and return a valid HTTP error
   response from it. If this cannot be done, return a standard 500 OLDError."
  [exception]
  (let [{ex-status :status
         ex-headers :headers
         ex-body :body :as ex-response} (ex-data exception)]
    (if (and ex-status ex-headers ex-body)
      ex-response
      (errors/error-code->response :old-unexpected-error))))

(defn handle
  "Given a stateful `application` component and a Ring `request` map, return a
   Ring response map. The `application` has the following keys
   - `:spec`: the OpenAPI spec map
   - `:operations`: a map from keywords to OpenAPI-operation-specific functions
     that implement the business logic of the request.
   High-level overview of the steps taken by this function.
   1. Attempt to recognize the OpenAPI operation targeted by the request.
   2. Validate the request according to the operation spec.
   3. Run the (global) security check(s) entailed by the OpenAPI spec using the
      `:security-handlers` of the `application`.
   4. Construct a response using the `:operations` handlers of the `application`.
   5. Validate the response according to the operation spec."
  [{:as application :keys [spec]} request]
  (let [[operationalized-context
         non-operationalized-error-response]
        (try [(recognize-operation request spec) nil]
             (catch Exception e [nil (exception->response e)]))]
    (if non-operationalized-error-response
      non-operationalized-error-response
      (let [operationalized-response (try (->> operationalized-context
                                               (security/run-security application)
                                               validate-request
                                               (run-operation application))
                                          (catch Exception e (exception->response e)))
            {:keys [response-validator]} operationalized-context]
        (try (response-validator operationalized-response)
             (catch Exception e (exception->response e)))))))
