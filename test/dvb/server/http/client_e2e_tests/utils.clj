(ns dvb.server.http.client-e2e-tests.utils
  (:require [dvb.server.core :as core]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.users :as db.users]
            [dvb.server.db.user-olds :as db.user-olds]
            [dvb.server.system.config :as config]
            [dvb.server.test-data :as test-data]))

(defn first-error-code [error]
  (-> error :errors first :error-code))

(defn first-data-error-code [error]
  (-> error :errors first :data :error-code))

(defn new-system []
  (core/make-main-system (assoc (config/init config/dev-config-path)
                                :server-port 8087)))

(defn setup [database]
  (let [{su-pwd :password :as superuser*}
        (test-data/gen-user-write {:is-superuser? true
                                   :created-by nil
                                   :updated-by nil})
        {su-id :id :as superuser} (db.users/activate-user
                                   database
                                   (db.users/create-user database superuser*))
        provenance {:created-by su-id :updated-by su-id}
        {u-pwd :password :as user*}
        (test-data/gen-user-write (assoc provenance :is-superuser? false))
        user (db.users/activate-user
              database
              (db.users/create-user database user*))
        {old-slug :slug :as old*} (assoc (test-data/gen-old-write provenance)
                                         :plan-id nil)
        old (db.olds/create-old database old*)]
    (db.user-olds/create-user-old
     database (merge provenance {:user-id (:id user)
                                 :old-slug old-slug
                                 :role "contributor"}))
    (db.user-olds/create-user-old
     database (merge provenance {:user-id su-id
                                 :old-slug old-slug
                                 :role "contributor"}))
    {:user user
     :user-password u-pwd
     :superuser superuser
     :superuser-password su-pwd
     :old old}))

(defn gen-ip []
  (str (rand-int 256) "." (rand-int 256) "." (rand-int 256) "." (rand-int 256)))
