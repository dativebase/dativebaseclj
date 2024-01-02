(ns dvb.server.repl
  (:require [clojure.pprint :as pp]
            [com.stuartsierra.component.repl :as component.repl]
            [dvb.common.openapi.spec :as spec]
            [dvb.client.core :as client]
            [dvb.server.core :as core]
            [dvb.server.db.events :as db.events]
            [dvb.server.db.forms :as db.forms]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.test-queries :as test-queries]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.openapi.serialize :as openapi.serialize]
            [dvb.server.sh :as sh]
            [dvb.server.system.config :as config]))

(defn new-system [_]
  (core/make-main-system (config/init config/dev-config-path)))

(component.repl/set-init new-system)

;; System Management
(comment

  ;; See https://github.com/stuartsierra/component.repl

  (component.repl/start) ;; Start the system

  (component.repl/stop) ;; Stop the system

  (component.repl/reset) ;; Reset (refresh) the system (reload all stale code)

  (keys component.repl/system) ;; the system
  '(:database :application :web-server)

)

;; OpenAPI stuff

(comment

  (do ;; evaluate entire do block to reload the OpenAPI spec

    (def openapi-path "resources/public/openapi/api.yaml")

    ;; Write the Clojure OpenAPI to disk as YAML:
    (openapi.serialize/clojure-openapi->disk spec/api openapi-path)

    ;; This will return nil if valid and throw if invalid
    (sh/lint-openapi openapi-path)

    )

)

;; Database Actions
(comment

  (do

    ;; DANGEROUS!!! Delete everything in the DB
    (test-queries/delete-all-the-things (:database component.repl/system))

    (def db (:database component.repl/system))

    (def email "uu1@gmail.com")

    (def password "uu1pw")

    ;; Create a new user, so we can login:
    (def user (db.users/create-user db {:first-name "User"
                                        :last-name "Utilizando"
                                        :email email
                                        :password password
                                        :is-superuser? true
                                        :created-by nil
                                        :updated-by nil}))

    (def users (db.users/get-users db))

    (def user-2 (db.users/create-user db {:first-name "Tim"
                                          :last-name "Benzyne"
                                          :email "tb@gmail.com"
                                          :password "123123"
                                          :is-superuser? false
                                          :created-by nil
                                          :updated-by nil}))

    (def fetched-user (db.users/get-user-by-email db email))

    ;; Create an OLD for our user to interact with
    (def old-slug "fra")

    (def fra-old (db.olds/create-old db {:slug old-slug
                                         :name "French OLD"
                                         :created-by nil
                                         :updated-by nil}))

    ;; Grant our user access to our OLD by creating a users_olds row:
    (def user-old (db.users/create-user-old db {:user-id (:id fetched-user)
                                                :old-slug old-slug
                                                :role "administrator"
                                                :created-by nil
                                                :updated-by nil}))

    ;; Get the user with their OLDs:
    (def user-with-roles (db.users/get-user-with-roles db (:id fetched-user)))

    )

)

(comment

  (def some-form-id "4e3ea6ee-c20a-4c37-869d-4a585180f046")

  (db.forms/get-form db some-form-id)

)

;; Client Interactions
(comment

  ;; Create an authenticated client by calling the LOGIN endpoint with the user
  ;; above's credentials:
  (def client (client/authenticate-client
               (client/make-client)
               email password))

  ;; The above will create a new row in the ``api_keys`` table.

  (:authenticated? client) ;; true (if the above worked)

  ;; This key is valid for 2 hours:
  (:api-key client)

  ;; Try to create a form through the client:
  (def create-form-response
    (client/create-form
     client
     old-slug
     {:transcription "chiens"}))

  ;; Update the form:
  (def update-form-response
    (client/update-form
     client
     old-slug
     (-> create-form-response :body :id)
     {:transcription "les chiens mangeaient."}))

  ;; Delete the form:
  (def delete-form-response
    (client/delete-form
     client
     old-slug
     (-> create-form-response :body :id)))

)

;; See How Immutable History Works
(comment

  (def user-2-email "uu2@gmail.com")

  (def user-2-password "uu2pw")

  ;; Create a user, then update it:
  (def updated-user
    (let [db (:database component.repl/system)
          test-user {:first-name "User"
                     :last-name "Utilizando"
                     :email user-2-email
                     :password user-2-password}
          created-user (db.users/create-user db test-user)
          new-user (assoc created-user :first-name "Anne")
          updated-user (db.users/update-user db new-user)]
      updated-user))

  (def user-2-id (:id updated-user))

  ;; View the updated user's history:
  (let [db (:database component.repl/system)
        user-id (:id updated-user)
        user (db.users/get-user db user-id)
        user-history (db.events/get-history db nil "users" user-id)]
    {:user user
     :user-history user-history})

  (db.users/get-user (:database component.repl/system) user-2-id)

  (db.users/get-user-by-email (:database component.repl/system) user-2-email)

  ;; View the history of transcription values for the form updated and deleted
  ;; above.
  (pp/pprint
   (->> (db.events/get-history db old-slug "forms"
                               (-> delete-form-response :body :id))
        (map (comp :transcription :row-data))))

)
