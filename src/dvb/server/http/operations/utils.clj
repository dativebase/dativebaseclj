(ns dvb.server.http.operations.utils
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.common.openapi.spec :as spec]))

(def user-required-keys
  (-> spec/api :components :schemas :User :required))

(defn minimize-user [user]
  (select-keys user user-required-keys))

(defn api-key-user-id [ctx]
  (-> ctx :security :api-key :user :id))

(defn security-user [ctx]
  (-> ctx :security :user))

(defn security-user-id [ctx]
  (:id (security-user ctx)))

(defn validate-entity-operation
  "Perform common checks for operations on a target entity. Throw an exception
  (triggering a 4XX-level response) if any of the following:
  - The specified entity could not be found.
  - The specified entity does not belong to the OLD referenced (in the path)."
  [{:keys [existing-entity entity-type entity-id old-slug operation]}]
  (when-not existing-entity
    (throw (errors/error-code->ex-info
            :entity-not-found
            {:old-slug old-slug
             :entity-type entity-type
             :entity-id entity-id
             :operation operation})))
  (when (not= old-slug (:old-slug existing-entity))
    (throw (errors/error-code->ex-info
            :old-slug-mismatch
            {:old-slug-from-path old-slug
             :old-slug-from-entity (:old-slug existing-entity)
             :operation operation}))))
