(ns dvb.server.http.operations.utils
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.server.db.users :as db.users]
            [dvb.server.log :as log]))

(defn minimize-user [user]
  (dissoc user :is-superuser? :email))

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

(defn validate-mutate-user-plan
  "Validator reused by operations create-user-plan, update-user-plan, and
  delete-user-plan."
  [mutation database {:as _user-plan :keys [user-id]} plan]
  (let [user (db.users/get-user database user-id)]
    (when-not user
      (let [data {:entity-type :user
                  :entity-id user-id
                  :operation mutation}]
        (log/warn "User not found" data)
        (throw (errors/error-code->ex-info :entity-not-found data))))
    (when-not plan
      (let [data {:entity-type :plan
                  :entity-id (:id plan)
                  :operation mutation}]
        (log/warn "Plan not found" data)
        (throw (errors/error-code->ex-info :entity-not-found data))))))

(defn validate-mutate-user-old
  "Validator reused by operations create-user-old, update-user-old, and
  delete-user-old."
  [mutation database {:as _user-old :keys [user-id]} old]
  (let [user (db.users/get-user database user-id)]
    (when-not user
      (let [data {:entity-type :user
                  :entity-id user-id
                  :operation mutation}]
        (log/warn "User not found" data)
        (throw (errors/error-code->ex-info :entity-not-found data))))
    (when-not old
      (let [data {:entity-type :old
                  :old-slug (:slug old)
                  :operation mutation}]
        (log/warn "OLD not found" data)
        (throw (errors/error-code->ex-info :entity-not-found data))))))
