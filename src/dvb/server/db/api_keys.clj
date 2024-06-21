(ns dvb.server.db.api-keys
  (:require [hugsql.core :as hugsql]))

(declare create-api-key*
         get-api-key*
         get-api-keys-for-user*
         delete-api-key*)

(hugsql/def-db-fns "sql/api_keys.sql")

(defn get-api-key [database id]
  (get-api-key* database {:id id}))

(defn create-api-key [database api-key]
  (create-api-key* database api-key))

(defn delete-api-key [database api-key]
  (delete-api-key* database api-key))

(defn get-api-keys-for-user [database user]
  (get-api-keys-for-user* database {:user-id (:id user)}))
