(ns dvb.common.edges.old-access-requests
  (:require [dvb.common.edges.common :as common]
            [dvb.common.utils :as utils]))

(def pg->clj-coercions {:status keyword
                        :old-slug keyword})

(def clj->pg-coercions {:status name
                        :old-slug name})

(def api->clj-coercions
  (merge common/api->clj-coercions
         {:user-id utils/str->uuid}
         pg->clj-coercions))

(def clj->api-coercions
  (merge common/clj->api-coercions
         {:user-id utils/uuid->str}
         clj->pg-coercions))

(defn api->clj [old-access-request]
  (common/perform-coercions old-access-request api->clj-coercions))

(defn clj->api [old-access-request]
  (-> old-access-request
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :OLDAccessRequest :properties keys))))

(defn pg->clj [old-access-request]
  (common/perform-coercions old-access-request pg->clj-coercions))

(defn clj->pg [old-access-request]
  (common/perform-coercions old-access-request clj->pg-coercions))

(defn create-api->clj [{:as response :keys [status]}]
  (if (= 201 status)
    (update response :body api->clj)
    response))

(defn write-clj->api [old-access-request-write]
  (-> old-access-request-write
      (common/perform-coercions clj->api-coercions)
      (select-keys (-> common/schemas :OLDAccessRequestWrite :properties keys))))

(defn fetch-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body api->clj)
    response))

(defn index-for-old-api->clj [{:as response :keys [status]}]
  (if (= 200 status)
    (update response :body (partial mapv api->clj))
    response))
