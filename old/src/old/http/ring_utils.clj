(ns old.http.ring-utils
  (:require [cheshire.core :as json]
            [old.http.openapi.errors :as openapi.errors]
            [old.http.utils :as utils]))

(def malformed-json-in-request-json-response
  (-> (openapi.errors/error-code->response :malformed-json-in-request-body)
      (update :body (comp json/encode utils/snake-stringify-keys))))

(defn wrap-cache-headers
  "The max-age=N response directive indicates that the response remains fresh
   until N seconds after the response is generated.
   The public response directive indicates that the response can be stored in a
   shared cache. Responses for requests with Authorization header fields must
   not be stored in a shared cache; however, the public directive will cause
   such responses to be stored in a shared cache."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Cache-Control"] "public, max-age=5"))))

(defn wrap-clojurify-request [handler]
  (fn [{:keys [body] :as request}]
    (handler
     (if body
       (update request :body utils/kebab-keywordize-keys)
       request))))

(defn wrap-declojurify-response [handler]
  (fn [request] (update (handler request) :body utils/snake-stringify-keys)))
