(ns dvb.server.http.repl
  "WARNING: Be careful with evaluating the functions in this namespace on a
   running OLD, especially in a production environment!"
  (:require cheshire.core
            [clj-http.client :as http.client]
            [com.stuartsierra.component :as component]
            [dvb.server.test-data :as test-data]))

(defn- parse-response [response]
  (-> response
      (select-keys [:status :headers :body])
      (update :headers select-keys [])))

(defn- http-create-form
  "Issue an actual HTTP request to to create a form in the local development OLD
   OpenAPI service, at the port specified in the spec, i.e.,
   http://localhost:8080. At present, the "
  [http-client-post {:keys [headers old-slug form url]}]
  (parse-response
   (http-client-post
    (format "%sapi/v1/%s/forms" url old-slug)
    {:headers headers
     :form-params (select-keys (test-data/gen-form form)
                               [:transcription])
     :as :json
     :accept :json
     :content-type :json})))

(defn- http-index-forms
  "Execute an actual HTTP request to fetch a page of forms from the local
   development OLD OpenAPI service."
  [http-client-get {:keys [headers old-slug url page items-per-page]
                    :or {page 0 items-per-page 10}}]
  (let [response
        (http-client-get
         (format "%sapi/v1/%s/forms" url old-slug)
         {:headers headers
          :query-params {:page page :items-per-page items-per-page}
          :as :json
          :accept :json
          :content-type :json})]
    (parse-response response)))

(defn- standard-params [old-client]
  (select-keys old-client [:url :old-slug :headers]))

(defprotocol IOLDClient
  "Methods for interacting with a stateful OLD web service."
  (index-forms [this parameters] "Request the forms of the OLD.")
  (create-form [this form] "Request the creation of a new form in the OLD."))

(defrecord OLDClient [url old-slug app-id api-key]
  component/Lifecycle
  (start [this]
    ;; api/v1/lan-old/forms"
    (-> this
        (update :headers merge {"X-API-KEY" api-key
                                "X-APP-ID" app-id
                                "Accept" "application/json"})))
  (stop [this]
    this)
  IOLDClient
  (index-forms [this parameters]
    (http-index-forms http.client/get
                      (merge (standard-params this) parameters)))
  (create-form [this form]
    (http-create-form http.client/post
                      (assoc (standard-params this) :form form))))

(comment

  (def old-client (component/start
                   (map->OLDClient {:url "http://localhost:8080/"
                                    :old-slug "lan-old"
                                    :app-id #uuid "fe447621-576f-4eea-bb1f-9ed5266e04eb"
                                    :api-key "dativeold"})))

  (component/stop old-client)

  ;; Get some forms

  (index-forms old-client {:page 0 :items-per-page 10})

  (index-forms old-client {:page 0 :items-per-page 50})

  ;; Create a new form and then fetch it (if there are fewer than 50 forms in
  ;; the OLD)

  (create-form old-client {:transcription "Joel"})

  (-> (index-forms old-client {:page 0 :items-per-page 50})
      :body :data
      reverse
      first)

)
