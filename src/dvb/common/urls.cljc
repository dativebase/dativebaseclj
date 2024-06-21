(ns dvb.common.urls
  "For building URLs for requests issued by various World Round-Up clients,
  e.g., a Clojure client (dvb.client) or a ClojureScript client (dvb.gui)."
  (:require [dvb.common.openapi.spec :as spec]
            [dvb.common.utils :as utils]))

(defn base-url-by-server-id [server-id]
  (-> (for [server spec/servers :when (= server-id (:id server))] server)
      first
      :url))

(def local-base-url (base-url-by-server-id :local))
(def local-docker-base-url (base-url-by-server-id :local-docker))
(def local-test-base-url (base-url-by-server-id :local-test))
(def prod-base-url (base-url-by-server-id :prod))

(def api-version "1")

;; OLD-independent URL constructors for abstract "resources"

(defn resources-url [resources-kw base-url]
  (utils/format "%s/api/v%s/%s"
                base-url api-version (utils/name-keyword-or-identity resources-kw)))

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
  (str (resource-url resources-kw
                     base-url
                     (utils/name-keyword-or-identity id))
       "/"
       (utils/name-keyword-or-identity suffix)))

(defn suffixed-paramed-resource-url [resources-kw suffix base-url id param]
  (str (suffixed-resource-url resources-kw suffix base-url id)
       "/" param))

;; OLD-dependent URL constructors for abstract "resources"

(defn old-specific-resources-url [resources-kw base-url old]
  (utils/format "%s/api/v%s/%s/%s"
                base-url
                api-version
                (utils/name-keyword-or-identity old)
                (utils/name-keyword-or-identity resources-kw)))

(defn old-specific-resource-url [resources-kw base-url old id]
  (str (old-specific-resources-url resources-kw base-url old)
       "/" id))

(defn new-old-specific-resource-url [resources-kw base-url old]
  (str (old-specific-resources-url resources-kw base-url old)
       "/new"))

(defn edit-old-specific-resource-url [resources-kw base-url old id]
  (str (old-specific-resource-url resources-kw base-url old id)
       "/edit"))

(defn cancel-old-specific-resource-url [resources-kw base-url old id]
  (str (old-specific-resource-url resources-kw base-url old id)
       "/cancel"))

;; Bespoke URLs

(defn login-url [base-url] (resources-url :login base-url))
(defn logout-url [base-url] (resources-url :logout base-url))
(defn sign-up-url [base-url] (resources-url :sign-up base-url))

;; OLD-specific Resource URLs

(def payments-url (partial old-specific-resources-url :payments))
(def payment-url (partial old-specific-resource-url :payments))
(def cancel-payment-url (partial cancel-old-specific-resource-url :payments))
(def stripe-webhooks-url (partial resources-url :stripe-webhooks))

;; OLD-non-specific Resource URLs

;; Users
(def user-url (partial resource-url :users))
(def users-url (partial resources-url :users))
(def edit-user-url (partial new-resource-url :users))
(def new-user-url (partial new-resource-url :users))
(def plans-for-user-url (partial suffixed-resource-url :users :plans))
(def deactivate-user-url (partial suffixed-resource-url :users :deactivate))
(def initiate-password-reset-url (partial suffixed-resource-url :users :initiate-password-reset))
(def reset-password-url (partial suffixed-resource-url :users :reset-password))
(def activate-user-url (partial suffixed-paramed-resource-url :users :activate))

;; Plans
(def plan-url (partial resource-url :plans))
(def plans-url (partial resources-url :plans))

;; OLDs
(defn old-url [base-url slug]
  (resource-url :olds base-url (utils/name-keyword-or-identity slug)))
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
