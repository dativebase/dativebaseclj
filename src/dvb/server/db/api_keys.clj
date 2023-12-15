(ns dvb.server.db.api-keys
  (:require [clojure.set :as set]
            [dvb.server.db.utils :as utils]
            [hugsql.core :as hugsql]))

(defn- api-key-row->api-key-entity [api-key-row]
  (set/rename-keys
   api-key-row
   {:user_id :user-id
    :created_at :created-at
    :expires_at :expires-at}))

(declare create-api-key*
         get-api-key*
         get-api-keys-for-user*
         delete-api-key*)

(hugsql/def-db-fns "sql/api_keys.sql")

(defn get-api-key [database id]
  (some-> (get-api-key* database {:id id})
          api-key-row->api-key-entity))

(defn create-api-key [database api-key]
  (some-> (create-api-key* database api-key)
          first
          api-key-row->api-key-entity))

(defn delete-api-key [database api-key]
  (some-> (delete-api-key* database api-key)
          first
          api-key-row->api-key-entity))

(defn get-api-keys-for-user [database user]
  (get-api-keys-for-user* database {:user-id (:id user)}))
