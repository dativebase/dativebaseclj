(ns dvb.common.edges.users
  (:require [clojure.set :as set]
            [dvb.common.edges.common :as common]
            [dvb.common.edges.olds-of-user :as olds-of-user]
            [dvb.common.edges.plans-of-user :as plans-of-user]
            [dvb.common.utils :as utils]))

(def clj->api-rename-keys {:is-superuser? :is-superuser})

(def api->clj-rename-keys (set/map-invert clj->api-rename-keys))

(def clj->pg-rename-keys clj->api-rename-keys)

(def pg->clj-rename-keys api->clj-rename-keys)

(def pg->clj-coercions
  {:registration-status keyword
   :plans plans-of-user/pgs->cljs
   :olds olds-of-user/pgs->cljs})

(def clj->pg-coercions
  {:registration-status name})

(def api->clj-coercions
  (merge (assoc common/api->clj-coercions
                :registration-key utils/str->uuid)
         pg->clj-coercions
         {:plans plans-of-user/apis->cljs}))

(def clj->api-coercions
  (merge (assoc common/clj->api-coercions
                :registration-key utils/uuid->str
                :plans plans-of-user/cljs->apis)
         clj->pg-coercions))

(defn clj->api [user]
  (-> user
      (common/perform-coercions clj->api-coercions)
      (set/rename-keys clj->api-rename-keys)
      (select-keys (-> common/schemas :User :properties keys))))

(defn user-password-reset-api->clj [user-password-reset]
  (-> user-password-reset
      (common/perform-coercions {:secret-key utils/str->uuid})
      (select-keys (-> common/schemas :UserPasswordReset :properties keys))))

(defn user-password-reset-clj->api [user-password-reset]
  (-> user-password-reset
      (common/perform-coercions {:secret-key utils/uuid->str})
      (select-keys (-> common/schemas :UserPasswordReset :properties keys))))

(defn write-clj->api [user-write]
  (-> user-write
      (common/perform-coercions clj->api-coercions)
      (set/rename-keys clj->api-rename-keys)
      (select-keys (-> common/schemas :UserWrite :properties keys))))

(defn update-clj->api [user-update]
  (-> user-update
      (common/perform-coercions clj->api-coercions)
      (set/rename-keys clj->api-rename-keys)
      (select-keys (-> common/schemas :UserUpdate :properties keys))))

(defn api->clj [user]
  (-> user
      (set/rename-keys api->clj-rename-keys)
      (common/perform-coercions api->clj-coercions)))

(defn pg->clj [user]
  (-> user
      (set/rename-keys pg->clj-rename-keys)
      (common/perform-coercions pg->clj-coercions)))

(defn create-api->clj [{:as response :keys [status]}]
  (if (= 201 status)
    (update response :body api->clj)
    response))

(defn fetch-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body api->clj)
    response))

(defn index-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update-in response [:body :data] (partial mapv api->clj))
    response))
