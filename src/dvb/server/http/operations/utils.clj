(ns dvb.server.http.operations.utils
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.plans :as db.plans]
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
  "Validator currently only used by operation create-user-old."
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

(defn validate-old-role-transition
  "Validate the transition of a user from from-role to to-role wrt OLD
  old-with-users. Users are prohibited from de-administrator-ing a user from an
  OLD when that user is that OLD's last administrator."
  [from-role to-role old-with-users]
  (let [administrators (db.olds/old-admins old-with-users)]
    (when (and (= :administrator from-role)
               (not= :administrator to-role)
               (<= (count administrators) 1))
      (let [message "Refusing to leave an OLD without at least one administrator. Please assign another user the administrator role on this OLD before retrying this request."
            data {:message message
                  :managers administrators
                  :error-code :no-administratorless-olds}]
        (log/warn message data)
        (throw (errors/error-code->ex-info :role-transition-violation data))))))

(defn validate-plan-role-transition
  "Validate the transition of a user from from-role to to-role wrt plan
  plan-with-members. Users are prohibited from de-manager-ing a user from a
  plan when that user is that plan's last manager."
  [from-role to-role plan-with-members]
  (let [managers (db.plans/plan-managers plan-with-members)]
    (when (and (= :manager from-role)
               (not= :manager to-role)
               (<= (count managers) 1))
      (let [message "Refusing to leave a plan without at least one manager. Please assign another user the manager role on this plan before retrying this request."
            data {:message message
                  :managers managers
                  :error-code :no-managerless-plans}]
        (log/warn message data)
        (throw (errors/error-code->ex-info :role-transition-violation data))))))

(defn remote-addr
  "This function exists solely for the purpose of having something we can stub out in tests."
  [request]
  (:remote-addr request))
