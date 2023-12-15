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

  (def client (authenticate-client
               (make-client)
               "ab@gmail.com"
               "abc"))

  (:authenticated? client)

  (def client (authenticate-client
               (make-client)
               "abc@bmail.com"
               "abc"))

  (def client (authenticate-client
               (make-client)
               "jrwdunham@gmail.com"
               "abcDE12!"))

  (dissoc client :spec)

  (show-user client "d4c12cef-c2fd-44ea-b365-7dd15ca338a3")

  (show-user client "x4c12cef-c2fd-44ea-b365-7dd15ca338a3") ;; invalid UUID

  (show-user client (:id (:user client)))

  (create-form client
                  {:name "Meta"
                   :work-type "mining"})

  (create-form client
                  {:name "Zoomba"
                   :work-type "cows"})

  (update-form client
                  "c23a65f0-d135-4358-bf69-c6f2f03f7754"
                  {:name "Zoomba"
                   :work-type "cows and hawses and varmints, oh my!"})

  (show-form client "0139d40b-e835-472b-a4b4-14c33aaa140a")

  (show-form client "7526b571-9a37-44f5-a139-06c36ab48dbf")

  (show-form client "a526b571-9a37-44f5-a139-06c36ab48dbf")

  (show-form client "abc")

  (show-form client "491f6f4f-bd22-4674-9a93-2a1c45976367")

  (delete-form client "491f6f4f-bd22-4674-9a93-2a1c45976367")

  (delete-form client "7526b571-9a37-44f5-a139-06c36ab48dbf")

  (delete-form client "21034ace-2abf-48c6-a071-ae9a7733ee96")

  ;; Production!!!

  (def prod-client (authenticate-client
                    (make-client :prod)
                    "jrwdunham@gmail.com"
                    "abcDE12!"))

  (keys prod-client)

  (dissoc prod-client :spec)

  (show-user prod-client "83fe6f05-d33d-42ce-8303-822239478b5b")

  (create-form prod-client
                  {:name "Meta"
                   :owner-id "83fe6f05-d33d-42ce-8303-822239478b5b"
                   :work-type "mining"})

  (create-form prod-client
                  {:name "Zoomba"
                   :owner-id "83fe6f05-d33d-42ce-8303-822239478b5b"
                   :work-type "surfing"})

  (create-form prod-client
                  {:name "Goomba"
                   :owner-id "83fe6f05-d33d-42ce-8303-822239478b5b"
                   :work-type "basting"})

  (show-form prod-client "3764db17-0148-43ac-9efe-148363c4aaf2")

  (update-form prod-client
                  "3764db17-0148-43ac-9efe-148363c4aaf2"
                  {:name "Goomba"
                   :work-type "basting and lambasting (and lamb-basting)"})

  (delete-form prod-client "3764db17-0148-43ac-9efe-148363c4aaf2")

  ;; FOX

  (show-form prod-client "8d7ee551-7fdf-44a4-8fea-b17f9993bfac")

  (show-form prod-client "abc")

  (show-form prod-client "ad7ee551-7fdf-44a4-8fea-b17f9993bfac")

  (delete-form prod-client "8d7ee551-7fdf-44a4-8fea-b17f9993bfac")

)
