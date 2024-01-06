(ns dvb.client.core
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as csk-extras]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvb.common.specs.olds :as old-specs]
            [dvb.common.specs.plans :as plan-specs]
            [dvb.common.specs.users :as user-specs]
            [dvb.common.specs.user-plans :as user-plan-specs]
            [dvb.common.edges :as edges]
            [dvb.common.openapi.serialize :as serialize]
            [dvb.common.openapi.spec :as spec]))

(def local-base-url
  (-> (for [server spec/servers :when (= :local (:id server))]
        server) first :url))

(def local-test-base-url
  (-> (for [server spec/servers :when (= :local-test (:id server))]
        server) first :url))

(def prod-base-url
  (-> (for [server spec/servers :when (= :prod (:id server))]
        server) first :url))

(defn forms-url [base-url old]
  (str base-url "/api/v1/" old "/forms"))

(defn new-form-url [base-url old]
  (str base-url "/api/v1/" old "/forms/new"))

(defn form-url [base-url old form-id]
  (str base-url "/api/v1/" old "/forms/" form-id))

(defn edit-form-url [base-url old form-id]
  (str base-url "/api/v1/" old "/forms/" form-id "/edit"))

(defn login-url [base-url] (str base-url "/api/v1/login"))

(defn user-url [base-url user-id]
  (str base-url "/api/v1/users/" user-id))

(defn plans-for-user-url [base-url user-id]
  (str base-url "/api/v1/users/" user-id "/plans"))

(defn activate-user-url [base-url user-id registration-key]
  (str base-url "/api/v1/users/" user-id "/activate/" registration-key))

(defn edit-user-url [base-url user-id]
  (str base-url "/api/v1/users/" user-id "/edit"))

(defn users-url [base-url] (str base-url "/api/v1/users"))

(defn user-plans-url [base-url] (str base-url "/api/v1/user-plans"))

(defn user-plan-url [base-url user-plan-id]
  (str base-url "/api/v1/user-plans/" user-plan-id))

(defn new-user-url [base-url]
  (str base-url "/api/v1/users/new"))

(defn plans-url [base-url] (str base-url "/api/v1/plans"))

(defn plan-url [base-url plan-id]
  (str base-url "/api/v1/plans/" plan-id))

(defn olds-url [base-url] (str base-url "/api/v1/olds"))

(defn old-url [base-url old-slug] (str base-url "/api/v1/olds/" old-slug))

(defn make-client
  ([] (make-client :local))
  ([type]
   (assert (some #{type} [:prod :local :local-test])
           "Type must be :prod, :local, or :local-test")
   (let [spec (serialize/denormalize spec/api)]
     {:spec spec
      :base-url (case type
                  :prod prod-base-url
                  :local local-base-url
                  :local-test local-test-base-url)})))

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

(defn login [client email password]
  (-> default-request
      (assoc :url (login-url (:base-url client))
             :method :post
             :body (json/encode {:email email :password password}))
      client/request
      simple-response))

;; TODO: create a persistent & stateful client using core.async. Rationale: the
;; client can indicate when its credentials are expired and prompt the user for
;; re-authentication.

(defn authenticate-client [client email password]
  (let [{:keys [status body]} (login client email password)]
    (if (= 200 status)
      (-> client
          (merge body)
          (assoc :authenticated? true))
      (assoc client :authenticated? false))))

(defn add-authentication-headers [request {:as _client :keys [api-key]}]
  (if api-key
    (update request :headers merge {"X-APP-ID" (:id api-key)
                                    "X-API-KEY" (:key api-key)})
    request))

(defn show-user
  "GET /users/<ID>"
  ([client user-id] (show-user client user-id {}))
  ([client user-id {:keys [include-plans?]
                    :or {include-plans? false}}]
   (-> default-request
       (assoc :url (user-url (:base-url client) user-id)
              :query-params {:include-plans include-plans?})
       (add-authentication-headers client)
       client/request
       simple-response
       edges/fetch-user-api->clj)))


(defn user-plans
  "GET /users/<ID>/plans"
  [client user-id]
  (-> default-request
      (assoc :url (plans-for-user-url (:base-url client) user-id))
      (add-authentication-headers client)
      client/request
      simple-response
      edges/fetch-user-plans-api->clj))

(defn edit-user
  "GET /users/<ID>/edit"
  [client user-id]
  (-> default-request
      (assoc :url (edit-user-url (:base-url client) user-id))
      (add-authentication-headers client)
      client/request
      simple-response))

(defn create-user
  "POST /users"
  [client user-write]
  (-> default-request
      (assoc :url (users-url (:base-url client))
             :method :post
             :body (json/encode
                    (edges/user-write-clj->api
                     (merge (gen/generate (s/gen ::user-specs/user-write))
                            user-write))))
      (add-authentication-headers client)
      client/request
      simple-response
      edges/create-user-api->clj))

(defn new-user
  "GET /users/new"
  [client]
  (-> default-request
      (assoc :url (new-user-url (:base-url client)))
      (add-authentication-headers client)
      client/request
      simple-response))

(defn update-user
  "PUT /users/<ID>"
  [client user-id user-update]
  (-> default-request
      (assoc :url (user-url (:base-url client) user-id)
             :method :put
             :body (json/encode user-update))
      (add-authentication-headers client)
      client/request
      simple-response
      edges/fetch-user-api->clj))

(defn activate-user
  "GET /users/<ID>/activate/<KEY>"
  [client user-id registration-key]
  (-> default-request
      (assoc :url (activate-user-url (:base-url client) user-id registration-key))
      client/request
      simple-response
      edges/fetch-user-api->clj))

;; NOTE: deliberately not implemented
#_(defn delete-user
  "DELETE /users/<ID>"
  [client user-id]
  (-> default-request
      (assoc :url (user-url (:base-url client) user-id)
             :method :delete)
      (add-authentication-headers client)
      client/request
      simple-response
      edges/fetch-user-api->clj))

(defn index-users
  "GET /users"
  ([client] (index-users client {}))
  ([client {:keys [page items-per-page]
            :or {page 0 items-per-page 10}}]
   (-> default-request
       (assoc :url (users-url (:base-url client))
              :query-params {:page page :items-per-page items-per-page})
       (add-authentication-headers client)
       client/request
       simple-response
       edges/index-users-api->clj)))

(defn create-old
  "POST /olds"
  [client old-write]
  (-> default-request
      (assoc :url (olds-url (:base-url client))
             :method :post
             :body (json/encode
                    (edges/old-write-clj->api
                     (merge (gen/generate (s/gen ::old-specs/old-write))
                            old-write))))
      (add-authentication-headers client)
      client/request
      simple-response
      edges/create-old-api->clj))

(defn update-old
  "PUT /olds/<SLUG>"
  [client old-slug old-update]
  (-> default-request
      (assoc :url (old-url (:base-url client) old-slug)
             :method :put
             :body (json/encode old-update))
      (add-authentication-headers client)
      client/request
      simple-response
      edges/fetch-old-api->clj))

(defn delete-old
  "DELETE /olds/<SLUG>"
  [client old-slug]
  (-> default-request
      (assoc :url (old-url (:base-url client) old-slug)
             :method :delete)
      (add-authentication-headers client)
      client/request
      simple-response
      edges/fetch-old-api->clj))

(defn show-old
  "GET /olds/<SLUG>"
  [client old-slug]
  (-> default-request
      (assoc :url (old-url (:base-url client) old-slug))
      (add-authentication-headers client)
      client/request
      simple-response
      edges/fetch-old-api->clj))

(defn create-plan
  "POST /plans"
  [client plan-write]
  (-> default-request
      (assoc :url (plans-url (:base-url client))
             :method :post
             :body (json/encode
                    (edges/plan-clj->api
                     (merge (gen/generate (s/gen ::plan-specs/plan-write))
                            plan-write))))
      (add-authentication-headers client)
      client/request
      simple-response
      edges/create-plan-api->clj))

(defn create-user-plan
  "POST /user-plans"
  [client user-plan-write]
  (-> default-request
      (assoc :url (user-plans-url (:base-url client))
             :method :post
             :body (json/encode
                    (edges/user-plan-clj->api
                     (merge (gen/generate (s/gen ::user-plan-specs/user-plan-write))
                            user-plan-write))))
      (add-authentication-headers client)
      client/request
      simple-response
      edges/create-user-plan-api->clj))

(defn update-user-plan
  "PUT /user-plans/<ID>"
  [client user-plan-id user-plan-update]
  (-> default-request
      (assoc :url (user-plan-url (:base-url client) user-plan-id)
             :method :put
             :body (json/encode user-plan-update))
      (add-authentication-headers client)
      client/request
      simple-response
      edges/fetch-user-plan-api->clj))

(defn delete-user-plan
  "DELETE /users-plan/<ID>"
  [client user-plan-id]
  (-> default-request
      (assoc :url (user-plan-url (:base-url client) user-plan-id)
             :method :delete)
      (add-authentication-headers client)
      client/request
      simple-response
      edges/fetch-user-plan-api->clj))

(defn delete-plan
  "DELETE /plans/<ID>"
  [client plan-id]
  (-> default-request
      (assoc :url (plan-url (:base-url client) plan-id)
             :method :delete)
      (add-authentication-headers client)
      client/request
      simple-response
      edges/fetch-plan-api->clj))

(defn show-plan
  "GET /plans/<ID>"
  ([client plan-id] (show-plan client plan-id {}))
  ([client plan-id {:keys [include-members?]
                    :or {include-members? false}}]
   (-> default-request
       (assoc :url (plan-url (:base-url client) plan-id)
              :query-params {:include-members include-members?})
       (add-authentication-headers client)
       client/request
       simple-response
       edges/fetch-plan-api->clj)))

(defn create-form
  "POST /forms"
  [client old form-write]
  (-> default-request
      (assoc :url (forms-url (:base-url client) old)
             :method :post
             :body (json/encode form-write))
      (add-authentication-headers client)
      client/request
      simple-response))

(defn new-form
  "GET /forms/new"
  [client old]
  (-> default-request
      (assoc :url (new-form-url (:base-url client) old))
      (add-authentication-headers client)
      client/request
      simple-response))

(defn edit-form
  "GET /forms/<ID>/edit"
  [client old form-id]
  (-> default-request
      (assoc :url (edit-form-url (:base-url client) old form-id))
      (add-authentication-headers client)
      client/request
      simple-response))

(defn show-form
  "GET /forms/<ID>"
  [client old form-id]
  (-> default-request
      (assoc :url (form-url (:base-url client) old form-id))
      (add-authentication-headers client)
      client/request
      simple-response))

(defn delete-form
  "DELETE /forms/<ID>"
  [client old form-id]
  (-> default-request
      (assoc :url (form-url (:base-url client) old form-id)
             :method :delete)
      (add-authentication-headers client)
      client/request
      simple-response))

(defn update-form
  "PUT /forms/<ID>"
  [client old form-id form-write]
  (-> default-request
      (assoc :url (form-url (:base-url client) old form-id)
             :method :put
             :body (json/encode form-write))
      (add-authentication-headers client)
      client/request
      simple-response))

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

  (delete-user client (:id created-user)) ;; 200 OK

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
