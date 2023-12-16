(ns dvb.server.http.operations.utils.declojurify
  (:require [dvb.common.openapi.spec :as spec]))

(def schemas (-> spec/api :components :schemas))

(defn maybe-instant->str [maybe-instant]
  (when maybe-instant (str maybe-instant)))

(defn common [entity]
  (-> entity
      (update :id str)
      (update :created-by str)
      (update :created-at maybe-instant->str)
      (update :updated-at maybe-instant->str)
      (update :destroyed-at maybe-instant->str)))

(defn user [user*]
  (-> user*
      common
      (dissoc :password)
      (select-keys (-> schemas :User :properties keys))))

(defn api-key [api-key*]
  (-> api-key*
      (update :id str)
      (update :user-id str)
      (update :created-at maybe-instant->str)
      (update :expires-at maybe-instant->str)))

(defn form [form*]
  (-> form*
      common
      (select-keys (-> schemas :Form :properties keys))))
