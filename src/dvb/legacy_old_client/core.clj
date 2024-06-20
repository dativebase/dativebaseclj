(ns dvb.legacy-old-client.core
  "Functionality for making requests to a legacy OLD (old-pyramid) instance."
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as csk-extras]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [dvb.legacy-old-client.utils :as u]
            [dvb.legacy-old-client.config :as c]
            [dvb.legacy-old-client.print :as p]))

(defn append-path
  "Append path to url."
  [url path]
  (format "%s/%s" (u/r-strip "/" url) (u/l-strip "/" path)))

(defn path-to-url
  "Convert path to a URL."
  [old-client path]
  (append-path (:url old-client) path))

(def default-request
  {:method :get
   :as :json
   :content-type :json
   :accept :json
   :coerce :always
   :throw-exceptions false
   :socket-timeout 3000
   :connection-timeout 3000})

(defn issue-request
  "Send out an HTTP request and return its body as parsed JSON."
  ([old-client method path]
   (issue-request old-client method path {} {}))
  ([old-client method path body-map]
   (issue-request old-client method path body-map {}))
  ([old-client method path body-map query-params]
   (let [url (path-to-url old-client path)
         request (cond-> (assoc default-request
                                :url url
                                :method method
                                :query-params query-params
                                :cookie-store (:cookie-store old-client))
                   (seq body-map)
                   (assoc :body (u/json-stringify body-map)))
         response (try
                    (client/request request)
                    (catch Exception e
                      (throw (ex-info "Failed requet to Legacy OLD"
                                      {:error-code :failed-request-to-legacy-old
                                       :request (dissoc request :cookie-store)
                                       :ex-data (ex-data e)
                                       :ex-message (ex-message e)}))))]
     (:body response))))

(defrecord OLDClient
  [url
   username
   password
   cookie-store])

(defn login
  "Issue a login request using old-client."
  [old-client]
  (issue-request
    old-client
    :post
    "login/authenticate"
    {:username (:username old-client)
     :password (:password old-client)}))

(defn make-old-client
  "Construct and return an OLDClient record, after attempting a login request."
  [& args]
  (let [old-client
        (map->OLDClient
          (into args {:url c/old-url-dflt
                      :username c/old-username-dflt
                      :password c/old-password-dflt
                      :cookie-store (clj-http.cookies/cookie-store)}))]
    (login old-client)
    old-client))

(defn oc-get
  "Issue an OLD Client GET request to path with optional query-params."
  ([old-client path] (oc-get old-client path {}))
  ([old-client path query-params]
   (issue-request old-client :get path {} query-params)))

(defn oc-post
  "Issue an OLD Client POST request to path with body-map as the
  JSON-stringified request body."
  [old-client path body-map]
  (issue-request old-client :post path body-map {}))

(defn oc-create
  "Issue an OLD Client POST request to path with body-map as the
  JSON-stringified request body."
  [old-client path body-map]
  (oc-post old-client path body-map))

(defn oc-put
  "Issue an OLD Client PUT request to path with body-map as the
  JSON-stringified request body."
  [old-client path body-map]
  (issue-request old-client :put path body-map))

(defn oc-update
  "Issue an OLD Client PUT request to path with body-map as the
  JSON-stringified request body."
  [old-client path body-map]
  (oc-put old-client path body-map))

(defn oc-delete
  "Issue an OLD Client DELETE request to path."
  [old-client path]
  (issue-request old-client :delete path))
