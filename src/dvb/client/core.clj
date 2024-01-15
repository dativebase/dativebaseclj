(ns dvb.client.core
  "Functionality for making requests to a DativeBase instance."
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as csk-extras]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [dvb.client.urls :as urls]
            [dvb.common.edges.olds :as old-edges]
            [dvb.common.edges.old-access-requests :as old-access-request-edges]
            [dvb.common.edges.plans :as plan-edges]
            [dvb.common.edges.users :as user-edges]
            [dvb.common.edges.user-olds :as user-old-edges]
            [dvb.common.edges.user-plans :as user-plan-edges]
            [dvb.common.openapi.validate :as validate]
            [dvb.common.openapi.serialize :as serialize]
            [dvb.common.openapi.spec :as spec]))

(def default-request
  {:method :get
   :as :json
   :content-type :json
   :accept :json
   :coerce :always
   :throw-exceptions false})

(def ->kebab (partial csk-extras/transform-keys csk/->kebab-case-keyword))

(defn simple-response [response]
  (-> response
      (select-keys [:status :body])
      ->kebab))

(defn- add-authentication-headers
  "Given a request map and a client map, if the client contains the api-key key,
  then add it to the headers of the request such that the request will be
  authenticated."
  [request {:as _client :keys [api-key]}]
  (if api-key
    (update request :headers merge {"X-APP-ID" (:id api-key)
                                    "X-API-KEY" (:key api-key)})
    request))

;; Request Builders
;;
;; These are functions used to build public API functions, such as show-form or
;; create-user.

(defn- construct-query-params [opts boolean-query-params]
  (when boolean-query-params
    (->> boolean-query-params
         (reduce
          (fn [acc [external-key api-key]]
            (let [option-value (get opts external-key false)]
              (assoc acc api-key option-value)))
          {}))))

(defn- show-resource
  "GET /<RESOURCES>/<ID>"
  ([config client id] (show-resource config client id {}))
  ([{:keys [method url-fn boolean-query-params api->clj-fn]
     :or {api->clj-fn identity}}
    {:as client :keys [base-url]} id opts]
   (-> (if method (assoc default-request :method method) default-request)
       (assoc :url (url-fn base-url id)
              :query-params (construct-query-params opts boolean-query-params))
       (add-authentication-headers client)
       client/request
       simple-response
       api->clj-fn)))

(defn- show-old-specific-resource
  "GET /<OLD_SLUG>/<RESOURCES>/<ID>"
  ([config client old-slug id]
   (show-old-specific-resource config client old-slug id {}))
  ([{:keys [method url-fn boolean-query-params api->clj-fn]
     :or {api->clj-fn identity}}
    {:as client :keys [base-url]} old-slug id opts]
   (-> (if method (assoc default-request :method method) default-request)
       (assoc :url (url-fn base-url old-slug id)
              :query-params (construct-query-params opts boolean-query-params))
       (add-authentication-headers client)
       client/request
       simple-response
       api->clj-fn)))

(defn- index-resources
  "GET /<RESOURCES>"
  ([config client] (index-resources config client {}))
  ([{:keys [url-fn api->clj-fn] :or {api->clj-fn identity}}
    {:as client :keys [base-url]}
    {:keys [page items-per-page] :or {page 0 items-per-page 10}}]
   (-> default-request
       (assoc :url (url-fn base-url)
              :query-params {:page page :items-per-page items-per-page})
       (add-authentication-headers client)
       client/request
       simple-response
       api->clj-fn)))

(defn- index-old-specific-resources
  "GET /<OLD_SLUG>/<RESOURCES>"
  ([config client old] (index-old-specific-resources config client old {}))
  ([{:keys [url-fn api->clj-fn] :or {api->clj-fn identity}}
    {:as client :keys [base-url]}
    old
    {:keys [page items-per-page] :or {page 0 items-per-page 10}}]
   (-> default-request
       (assoc :url (url-fn base-url old)
              :query-params {:page page :items-per-page items-per-page})
       (add-authentication-headers client)
       client/request
       simple-response
       api->clj-fn)))

(defn- new-resource
  "GET /<RESOURCES>/new"
  [{:keys [url-fn api->clj-fn] :or {api->clj-fn identity}}
   {:as client :keys [base-url]}]
  (-> default-request
      (assoc :url (url-fn base-url))
      (add-authentication-headers client)
      client/request
      simple-response
      api->clj-fn))

(defn- new-old-specific-resource
  "GET /<OLD_SLUG>/<RESOURCES>/new"
  [{:keys [url-fn api->clj-fn] :or {api->clj-fn identity}}
   {:as client :keys [base-url]} old]
  (-> default-request
      (assoc :url (url-fn base-url old))
      (add-authentication-headers client)
      client/request
      simple-response
      api->clj-fn))

(defn- delete-resource
  "DELETE /<RESOURCES>/<ID>"
  ([config client id] (delete-resource config client id {}))
  ([config client id opts]
   (show-resource (assoc config :method :delete) client id opts)))

(defn- delete-old-specific-resource
  "DELETE /<OLD_SLUG>/<RESOURCES>/<ID>"
  ([config client old-slug id]
   (delete-old-specific-resource config client old-slug id {}))
  ([config client old-slug id opts]
   (show-old-specific-resource
    (assoc config :method :delete) client old-slug id opts)))

(defn- write-payload
  "Construct the write payload to be sent as the JSON body of a POST request.
  Validate the payload here if `openapi-schema` is not nil.
  - `resource-write`: the payload (map, with Clojure conventions) provided by
    the caller.
  - `clj->api-fn`: a function that can transform the payload from Clojure
    conventions to API conventions.
  - `openapi-schema`: (nillable map) the OpenAPI schema for validating the
    resulting payload before sending it to the server."
  [resource-write clj->api-fn openapi-schema]
  (cond-> resource-write
    :always clj->api-fn
    openapi-schema (validate/validate openapi-schema [])))

(defn- create-resource
  "POST /<RESOURCES>"
  [{:keys [url-fn create-api->clj-fn write-clj->api-fn openapi-schema]
    :or {create-api->clj-fn identity
         write-clj->api-fn identity}}
   {:as client :keys [base-url spec]} resource-write]
  (let [payload (write-payload resource-write write-clj->api-fn
                               (and openapi-schema
                                    (-> spec :components :schemas
                                        openapi-schema)))]
    (-> default-request
        (assoc :url (url-fn base-url)
               :method :post
               :body (json/encode payload))
        (add-authentication-headers client)
        client/request
        simple-response
        create-api->clj-fn)))

(defn- create-old-specific-resource
  "POST /<OLD_SLUG>/<RESOURCES>"
  [{:keys [url-fn create-api->clj-fn write-clj->api-fn openapi-schema]
    :or {create-api->clj-fn identity
         write-clj->api-fn identity}}
   {:as client :keys [base-url spec]} old-slug resource-write]
  (let [payload (write-payload resource-write write-clj->api-fn
                               (and openapi-schema
                                    (-> spec :components :schemas
                                        openapi-schema)))]
    (-> default-request
        (assoc :url (url-fn base-url old-slug)
               :method :post
               :body (json/encode payload))
        (add-authentication-headers client)
        client/request
        simple-response
        create-api->clj-fn)))

(defn- update-resource
  "PUT /<RESOURCES>/<ID>"
  [{:keys [url-fn fetch-api->clj-fn update-clj->api-fn openapi-schema]
    :or {fetch-api->clj-fn identity
         update-clj->api-fn identity}}
   {:as client :keys [base-url spec]} id resource-update]
  (let [payload (write-payload resource-update update-clj->api-fn
                               (and openapi-schema
                                    (-> spec :components :schemas
                                        openapi-schema)))]
    (-> default-request
        (assoc :url (url-fn base-url id)
               :method :put
               :body (json/encode payload))
        (add-authentication-headers client)
        client/request
        simple-response
        fetch-api->clj-fn)))

(defn- update-old-specific-resource
  "PUT /<OLD_SLUG>/<RESOURCES>/<ID>"
  [{:keys [url-fn fetch-api->clj-fn update-clj->api-fn openapi-schema]
    :or {fetch-api->clj-fn identity
         update-clj->api-fn identity}}
   {:as client :keys [base-url spec]} old id resource-update]
  (let [payload (write-payload resource-update update-clj->api-fn
                               (and openapi-schema
                                    (-> spec :components :schemas
                                        openapi-schema)))]
    (-> default-request
        (assoc :url (url-fn base-url old id)
               :method :put
               :body (json/encode payload))
        (add-authentication-headers client)
        client/request
        simple-response
        fetch-api->clj-fn)))

(defn- mutate-old-access-request
  "PUT /old-access-requests/<ID>/<MUTATION>"
  [mutation client old-access-request-id]
  (-> default-request
      (assoc :url (str (urls/old-access-request-url (:base-url client)
                                                    old-access-request-id)
                       "/" (name mutation))
             :method :put)
      (add-authentication-headers client)
      client/request
      simple-response
      old-access-request-edges/fetch-api->clj))

;; Public API

(defn make-client
  ([] (make-client :local))
  ([type]
   (assert (some #{type} [:prod :local :local-test])
           "Type must be :prod, :local, or :local-test")
   (let [spec (serialize/denormalize spec/api)]
     {:spec spec
      :base-url (case type
                  :prod urls/prod-base-url
                  :local urls/local-base-url
                  :local-test urls/local-test-base-url)})))

(defn login [client email password]
  (-> default-request
      (assoc :url (urls/login-url (:base-url client))
             :method :post
             :body (json/encode {:email email :password password}))
      client/request
      simple-response))

(defn authenticate-client [client email password]
  (let [{:keys [status body]} (login client email password)]
    (if (= 200 status)
      (-> client
          (merge body)
          (assoc :authenticated? true))
      (assoc client :authenticated? false))))

;; Show Resource: GET /<RESOURCES>/<ID>

(def show-user
  (partial show-resource
           {:url-fn urls/user-url
            :api->clj-fn user-edges/fetch-api->clj
            :boolean-query-params {:include-plans? :include-plans}}))

(def show-old
  (partial show-resource
           {:url-fn urls/old-url
            :api->clj-fn old-edges/fetch-api->clj
            :boolean-query-params {:include-users? :include-users}}))

(def show-plan
  (partial show-resource
           {:url-fn urls/plan-url
            :api->clj-fn plan-edges/fetch-api->clj
            :boolean-query-params {:include-members? :include-members
                                   :include-olds? :include-olds}}))

(def show-old-access-request
  (partial show-resource
           {:url-fn urls/old-access-request-url
            :api->clj-fn old-access-request-edges/fetch-api->clj}))

(def show-form
  (partial show-old-specific-resource
           {:url-fn urls/form-url}))

;; Delete Resource: DELETE /<RESOURCES>/<ID>
;;
;; We can reuse the show-resource and show-old-specific-resource helpers for
;; these requests.

(def delete-user
  (partial delete-resource
           {:url-fn urls/user-url
            :api->clj-fn user-edges/fetch-api->clj}))

(def delete-old
  (partial delete-resource
           {:url-fn urls/old-url
            :api->clj-fn old-edges/fetch-api->clj}))

(def delete-plan
  (partial delete-resource
           {:url-fn urls/plan-url
            :api->clj-fn plan-edges/fetch-api->clj}))

(def delete-user-plan
  (partial delete-resource
           {:url-fn urls/user-plan-url
            :api->clj-fn user-plan-edges/fetch-api->clj}))

(def delete-form
  (partial delete-old-specific-resource
           {:url-fn urls/form-url}))

;; Edit Resources: GET /<RESOURCES>/<ID>/edit
;;
;; We can reuse the show-resource and show-old-specific-resource helpers for
;; these GET /<RESOURCES>/<ID>/edit requests.

(def edit-user (partial show-resource {:url-fn urls/edit-user-url}))

(def edit-form (partial show-old-specific-resource {:url-fn urls/edit-form-url}))

;; New Resource: GET /<RESOURCES>/new

(def new-user (partial new-resource {:url-fn urls/new-user-url}))

(def new-form (partial new-old-specific-resource {:url-fn urls/new-form-url}))

;; Create Resources

(def create-user
  (partial create-resource
           {:url-fn urls/users-url
            :create-api->clj-fn user-edges/create-api->clj
            :write-clj->api-fn user-edges/write-clj->api
            :openapi-schema :UserWrite}))

(def create-plan
  (partial create-resource
           {:url-fn urls/plans-url
            :create-api->clj-fn plan-edges/create-api->clj
            :write-clj->api-fn plan-edges/write-clj->api
            :openapi-schema :PlanWrite}))

(def create-old
  (partial create-resource
           {:url-fn urls/olds-url
            :create-api->clj-fn old-edges/create-api->clj
            :write-clj->api-fn old-edges/write-clj->api
            :openapi-schema :OLDWrite}))

(def create-user-plan
  (partial create-resource
           {:url-fn urls/user-plans-url
            :create-api->clj-fn user-plan-edges/create-api->clj
            :write-clj->api-fn user-plan-edges/clj->api
            :openapi-schema :UserPlanWrite}))

(def create-user-old
  (partial create-resource
           {:url-fn urls/user-olds-url
            :create-api->clj-fn user-old-edges/create-api->clj
            :write-clj->api-fn user-old-edges/clj->api
            :openapi-schema :UserOLDWrite}))

(def create-old-access-request
  (partial create-resource
           {:url-fn urls/old-access-requests-url
            :create-api->clj-fn old-access-request-edges/create-api->clj
            :write-clj->api-fn old-access-request-edges/write-clj->api
            :openapi-schema :OLDAccessRequestWrite}))

(def create-form
  (partial create-old-specific-resource
           {:url-fn urls/forms-url
            :openapi-schema :FormWrite}))

;; Update Resource: PUT /<RESOURCES>/<ID>

(def update-user
  (partial update-resource
           {:url-fn urls/user-url
            :update-clj->api-fn user-edges/update-clj->api
            :fetch-api->clj-fn user-edges/fetch-api->clj
            :openapi-schema :UserUpdate}))

(def update-old
  (partial update-resource
           {:url-fn urls/old-url
            :update-clj->api-fn old-edges/update-clj->api
            :fetch-api->clj-fn old-edges/fetch-api->clj
            :openapi-schema :OLDUpdate}))

(def update-user-plan
  (partial update-resource
           {:url-fn urls/user-plan-url
            :update-clj->api-fn user-plan-edges/update-clj->api
            :fetch-api->clj-fn user-plan-edges/fetch-api->clj
            :openapi-schema :UserPlanUpdate}))

(def update-user-old
  (partial update-resource
           {:url-fn urls/user-old-url
            :update-clj->api-fn user-old-edges/update-clj->api
            :fetch-api->clj-fn user-old-edges/fetch-api->clj
            :openapi-schema :UserOLDUpdate}))

(def update-form
  (partial update-old-specific-resource
           {:url-fn urls/form-url}))

;; Bespoke / Custom Operations

(def deactivate-user
  "GET /users/<ID>/deactivate"
  (partial show-resource
           {:url-fn urls/deactivate-user-url
            :api->clj-fn user-edges/fetch-api->clj}))

(defn activate-user
  "GET /users/<ID>/activate/<KEY>"
  [{:as _client :keys [base-url]} user-id registration-key]
  (-> default-request
      (assoc :url (urls/activate-user-url base-url user-id registration-key))
      client/request
      simple-response
      user-edges/fetch-api->clj))

;; Index Resources: GET /<RESOURCES

(def index-users
  (partial index-resources {:url-fn urls/users-url
                            :api->clj-fn user-edges/index-api->clj}))

(def index-olds
  (partial index-resources {:url-fn urls/olds-url
                            :api->clj-fn old-edges/index-api->clj}))

(def index-forms
  (partial index-old-specific-resources
           {:url-fn urls/forms-url}))

;; Miscellaneious Operations

(def access-requests-for-old
  (partial show-resource
           {:url-fn urls/access-requests-for-old-url
            :api->clj-fn old-access-request-edges/index-for-old-api->clj}))

(def approve-old-access-request (partial mutate-old-access-request :approve))
(def reject-old-access-request (partial mutate-old-access-request :reject))
(def retract-old-access-request (partial mutate-old-access-request :retract))

(defn user-plans
  "GET /users/<ID>/plans"
  [client user-id]
  (-> default-request
      (assoc :url (urls/plans-for-user-url (:base-url client) user-id))
      (add-authentication-headers client)
      client/request
      simple-response
      user-plan-edges/fetch-api->clj))

(comment

  (do ;; superuser client
    (def email "uu1@gmail.com")
    (def password "uu1pw")
    (def old-slug "fra")
    (def client (authenticate-client (make-client) email password)))

  (do ;; regular user client
    (def email "tb@gmail.com")
    (def password "123123")
    (def old-slug "fra")
    (def client (authenticate-client (make-client) email password)))

  (:authenticated? client)

  (new-form client old-slug)

  (edit-form client old-slug (str (random-uuid)))

  (new-user client)

  (edit-user client "1a283b6c-553d-49be-bab9-0654a5557eec")

  (def user-email (str (random-uuid) "@gmail.com"))

  (def created-user
    (:body (create-user client
                        {:first-name (str (random-uuid))
                         :last-name (str (random-uuid))
                         :email user-email
                         :password "2222"
                         :is-superuser? false})))

  (show-user client (:id created-user)) ;; 200 OK

  #_(delete-user client (:id created-user)) ;; 200 OK

  (dissoc client :spec)

  (show-user client "x4c12cef-c2fd-44ea-b365-7dd15ca338a3") ;; invalid UUID: 400

  (show-user client (:id (:user client))) ;; whoami?

  (update-user client (:id (:user client)) {:first-name "Judy"})

  (def created-user (create-user client
                                 {:first-name "ZZ"
                                  :last-name "Top"
                                  :email "zzt@gmail.com"
                                  :password "2222"
                                  :is-superuser true}))

  (def created-non-superuser (create-user client
                                          {:first-name "Not"
                                           :last-name "Super"
                                           :email "notsup@gmail.com"
                                           :password "2222"
                                           :is-superuser false}))

  (def created-form (create-form client
                                 old-slug
                                 {:transcription "Les ..."}))

  (def updated-form (update-form client
                                 old-slug
                                 (-> created-form :body :id)
                                 {:transcription "Les chiens ..."})) ;; NOT idempotent

  (index-users client)

  [(index-users client {:page 0 :items-per-page 1})
   (index-users client {:page 1 :items-per-page 1})
   (index-users client {:page 2 :items-per-page 1})
   (index-users client {:page 3 :items-per-page 1})]

  (show-form client old-slug (-> created-form :body :id)) ;; 200

  (show-form client old-slug (str (random-uuid))) ;; 404

  (show-form client old-slug "abc") ;; 400: not a valid UUID

  (def deleted-form (delete-form client
                                 old-slug
                                 (-> created-form :body :id))) ;; 200; not idempotent: repeat triggers 404 cuz form is technically no longer extant

  (delete-form client old-slug "foo")

)

;; Production API Interactions. Be Careful!!!
(comment

  (def prod-client (authenticate-client
                    (make-client :prod)
                    "REDACTED"
                    "REDACTED"))

  (keys prod-client)

  (dissoc prod-client :spec)

)
