(ns dvb.client.core
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as csk-extras]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [dvb.common.openapi.serialize :as serialize]
            [dvb.common.openapi.spec :as spec]
            #_[dvb.common.openapi.serialize :as serialize]
            #_[dvb.common.openapi.spec :as spec]))

(def local-base-url
  (-> (for [server spec/servers :when (= :local (:id server))]
        server) first :url))

(def prod-base-url
  (-> (for [server spec/servers :when (= :prod (:id server))]
        server) first :url))

(defn forms-url [base-url old]
  (str base-url "/api/v1/" old "/forms"))

(defn form-url [base-url old form-id]
  (str base-url "/api/v1/" old "/forms/" form-id))

(defn login-url [base-url] (str base-url "/api/v1/login"))

(defn user-url [base-url user-id]
  (str base-url "/api/v1/users/" user-id))

(defn make-client
  ([] (make-client :local))
  ([type]
   (assert (some #{type} [:prod :local]) "Type must be :prod or :local")
   (let [spec (serialize/denormalize spec/api)]
     {:spec spec
      :base-url (if (= :prod type) prod-base-url local-base-url)})))

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

(defn authenticate [request {:as _client :keys [api-key]}]
  (update request :headers merge {"X-APP-ID" (:id api-key)
                                  "X-API-KEY" (:key api-key)}))

(defn show-user
  "GET /users/<ID>"
  [client user-id]
  (-> default-request
      (assoc :url (user-url (:base-url client) user-id))
      (authenticate client)
      client/request
      simple-response))

(defn create-form
  "POST /forms"
  [client old form-write]
  (-> default-request
      (assoc :url (forms-url (:base-url client) old)
             :method :post
             :body (json/encode form-write))
      (authenticate client)
      client/request
      simple-response))

(defn show-form
  "GET /forms/<ID>"
  [client old form-id]
  (-> default-request
      (assoc :url (form-url (:base-url client) old form-id))
      (authenticate client)
      client/request
      simple-response))

(defn delete-form
  "DELETE /forms/<ID>"
  [client old form-id]
  (-> default-request
      (assoc :url (form-url (:base-url client) old form-id)
             :method :delete)
      (authenticate client)
      client/request
      simple-response))

(defn update-form
  "PUT /forms/<ID>"
  [client old form-id form-write]
  (-> default-request
      (assoc :url (form-url (:base-url client) old form-id)
             :method :put
             :body (json/encode form-write))
      (authenticate client)
      client/request
      simple-response))

(comment

  (def email "uu1@gmail.com")

  (def password "uu1pw")

  (def old-slug "fra")

  (def client (authenticate-client (make-client) email password))

  (:authenticated? client)

  (dissoc client :spec)

  (def contingent-user-id "5c76c8cd-ac06-4f44-a60d-571da90b2f5e")

  (show-user client contingent-user-id) ;; 200 OK

  (show-user client "x4c12cef-c2fd-44ea-b365-7dd15ca338a3") ;; invalid UUID: 400

  (show-user client (:id (:user client))) ;; whoami?

  (def created-form (create-form client
                                 old-slug
                                 {:transcription "Les ..."}))

  (def updated-form (update-form client
                                 old-slug
                                 (-> created-form :body :id)
                                 {:transcription "Les chiens ..."})) ;; NOT idempotent

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
