(ns dvb.client.init-local
  "Client namespace for initializing a locally running system"
  (:require [com.stuartsierra.component.repl :as component.repl]
            [dvb.server.db.users :as db.users]
            [dvb.client.core :as c]))

;; Initialization means creating the following OLD and user.

(def old :rus)
(def email "some@email.com")
(def password "vD7R01a921zWs7qesdf;ljztSORNgJt")
;; vD7R01a921zWs7qesdf;ljztSORNgJt

(def user
  {:first-name "Some"
   :last-name "User"
   :email email
   :password password
   :is-superuser? false})

;; Ensure we have a user with which we can authenticate.
;; Create a client for local.
(comment

  (def db (:database component.repl/system))

  (def unauthenticated-local-client (c/make-client))

  (def new-user-response (c/create-user unauthenticated-local-client user))

  (def user (db.users/get-user db (-> new-user-response :body :id)))
  (def user-id (:id user))
  (def registration-key (:registration-key user))

  (def activate-user-response
    (c/activate-user unauthenticated-local-client user-id registration-key))

  (def client
    (c/authenticate-client unauthenticated-local-client email password))

  (:authenticated? client) ;; true

)

;; Ensure we have an OLD
(comment

  ;; Count extant olds:
  ;; (There were 303 on Feb 29, 2024, from test runs.)
  (-> (c/index-olds client) :body :meta :count) ;; 303

  ;; Create an OLD:
  (def created-old-response
    (c/create-old
     client
     {:slug old
      :name "Russian OLD"
      :year 2024}))
  {:status 201
   :body
   {:updated-at "2024-02-29T23:11:11.992687Z"
    :slug :rus
    :name "Russian OLD"
    :plan-id nil
    :year 2024
    :updated-by #uuid "5899dffb-5c67-4b8e-9292-e19e691369fe"
    :created-by #uuid "5899dffb-5c67-4b8e-9292-e19e691369fe"
    :destroyed-at nil
    :created-at "2024-02-29T23:11:11.992687Z"
    :users
    [{:id #uuid "5899dffb-5c67-4b8e-9292-e19e691369fe"
      :user-old-id #uuid "c23ebc43-bc84-4f09-9623-64e365e26fc2"
      :role :administrator}]}}

  (c/index-registration-forms client old)
  ;; {:status 200 :body {:data [] :meta {:count 0 :page 0 :items-per-page 10}}}

  ;; Now register in the UI at http://localhost:61000/register and then re-index
  ;; the registration forms, as above.
  (c/index-registration-forms client old)
  {:status 200
   :body
   {:data
    [{:id #uuid "48e4b65f-e91b-475f-8aa8-e02e0e451a51",,,}]
    :meta {:count 1 :page 0 :items-per-page 10}}}

  ;; Count registration forms:
  (-> (c/index-registration-forms client old) :body :meta :count) ;; 9

)
