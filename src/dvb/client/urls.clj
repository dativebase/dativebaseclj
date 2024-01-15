(ns dvb.client.urls
  "For building URLs for the requests issued by the DativeBase client."
  (:require [dvb.common.openapi.spec :as spec]))

(def local-base-url
  (-> (for [server spec/servers :when (= :local (:id server))]
        server) first :url))

(def local-test-base-url
  (-> (for [server spec/servers :when (= :local-test (:id server))]
        server) first :url))

(def prod-base-url
  (-> (for [server spec/servers :when (= :prod (:id server))]
        server) first :url))

(def api-version "1")

;; OLD-independent URL constructors for abstract "resources"

(defn resources-url [resources-kw base-url]
  (format "%s/api/v%s/%s"
          base-url api-version (name resources-kw)))

(defn resource-url [resources-kw base-url id]
  (str (resources-url resources-kw base-url)
       "/" id))

(defn new-resource-url [resources-kw base-url]
  (str (resources-url resources-kw base-url)
       "/new"))

(defn edit-resource-url [resources-kw base-url id]
  (str (resource-url resources-kw base-url id)
       "/edit"))

(defn suffixed-resource-url [resources-kw suffix base-url id]
  (str (resource-url resources-kw base-url id)
       "/" (name suffix)))

(defn suffixed-paramed-resource-url [resources-kw suffix base-url id param]
  (str (suffixed-resource-url resources-kw suffix base-url id)
       "/" param))

;; OLD-dependent URL constructors for abstract "resources"

(defn old-specific-resources-url [resources-kw base-url old]
  (format "%s/api/v%s/%s/%s"
          base-url api-version old (name resources-kw)))

(defn old-specific-resource-url [resources-kw base-url old id]
  (str (old-specific-resources-url resources-kw base-url old)
       "/" id))

(defn new-old-specific-resource-url [resources-kw base-url old]
  (str (old-specific-resources-url resources-kw base-url old)
       "/new"))

(defn edit-old-specific-resource-url [resources-kw base-url old id]
  (str (old-specific-resource-url resources-kw base-url old id)
       "/edit"))

;; Bespoke URLs

(defn login-url [base-url] (str base-url "/api/v1/login"))

;; OLD-specific Resource URLs

(def forms-url (partial old-specific-resources-url :forms))
(def form-url (partial old-specific-resource-url :forms))
(def new-form-url (partial new-old-specific-resource-url :forms))
(def edit-form-url (partial edit-old-specific-resource-url :forms))

;; OLD-non-specific Resource URLs

;; Users
(def user-url (partial resource-url :users))
(def users-url (partial resources-url :users))
(def edit-user-url (partial new-resource-url :users))
(def new-user-url (partial new-resource-url :users))
(def plans-for-user-url (partial suffixed-resource-url :users :plans))
(def deactivate-user-url (partial suffixed-resource-url :users :deactivate))
(def activate-user-url (partial suffixed-paramed-resource-url :users :activate))

;; Plans
(def plan-url (partial resource-url :plans))
(def plans-url (partial resources-url :plans))

;; OLDs
(def old-url (partial resource-url :olds))
(def olds-url (partial resources-url :olds))
(def access-requests-for-old-url (partial suffixed-resource-url :olds :access-requests))

;; User Plans
(def user-plan-url (partial resource-url :user-plans))
(def user-plans-url (partial resources-url :user-plans))

;; User OLDs
(def user-old-url (partial resource-url :user-olds))
(def user-olds-url (partial resources-url :user-olds))

;; OLD Access Requests
(def old-access-request-url (partial resource-url :old-access-requests))
(def old-access-requests-url (partial resources-url :old-access-requests))
