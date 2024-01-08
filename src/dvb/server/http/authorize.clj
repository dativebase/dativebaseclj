(ns dvb.server.http.authorize
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.http.operations.utils :as u]
            [dvb.server.log :as log]))

(def operation-roles
  "Map from operation IDs to collections of roles that are authorized to execute
   the operation. If the roles collection is `[:*]`, then all roles are
   authorized for the operation."
  {:index-forms [:*]
   :create-form [:administrator :contributor]
   :delete-form [:administrator :contributor]
   :edit-form [:administrator :contributor]
   :new-form [:administrator :contributor]
   :show-form [:*]
   :update-form [:administrator :contributor]})

(def old-independent-operations
  #{:create-old
    :create-plan
    :create-user
    :create-user-old
    :create-user-plan
    :delete-plan
    :delete-old
    :delete-user
    :delete-user-old
    :delete-user-plan
    :edit-user
    :index-olds
    :index-users
    :new-user
    :show-old
    :show-plan
    :show-user
    :update-old
    :update-user
    :update-user-old
    :update-user-plan
    :user-plans})

(def superuser-operations
  #{})

(defn authorize [ctx]
  (let [{:keys [roles is-superuser?] user-id :id} (u/security-user ctx)
        old-slug (-> ctx :path :old_slug)
        operation-id (-> ctx :operation :operation-id)
        old-independent-operation? (boolean (operation-id old-independent-operations))
        role-for-old (get roles old-slug)]
    (when (and (not old-independent-operation?)
               (not role-for-old))
      (log/warn "Authenticated user has no roles in the target OLD."
                {:user-id user-id
                 :old old-slug
                 :operation-id operation-id})
      (throw (errors/error-code->ex-info :unauthorized)))
    (when old-independent-operation?
      (when (and (some #{operation-id} superuser-operations)
                 (not is-superuser?))
        (log/warn "Authenticated user is not a superuser. Operation prohibited."
                  {:user-id user-id
                   :operation-id operation-id})
        (throw (errors/error-code->ex-info :unauthorized))))
    (when-not old-independent-operation?
      (let [current-operation-roles (operation-id operation-roles)]
        (when-not (or (= [:*] current-operation-roles)
                      (some #{role-for-old} current-operation-roles))
          (log/warn "Authenticated user lacks the role required for the target operation."
                    {:user-id user-id
                     :old old-slug
                     :operation-id operation-id
                     :operation-roles current-operation-roles})
          (throw (errors/error-code->ex-info :unauthorized)))))))

(defn user-authorized-to-administer-old?
  "Return true if, and only if, the provided user is authorized to administer
  the provided OLD, i.e., only if the user is an administrator of the OLD."
  [{:as _authenticated-user authenticated-user-id :id :keys [is-superuser?]} old]
  (boolean (or is-superuser?
               (some #{authenticated-user-id} (db.olds/old-admins old)))))

(defn user-authorized-to-manage-plan?
  [{:as _authenticated-user authenticated-user-id :id :keys [is-superuser?]}
   plan]
  (boolean (or is-superuser?
               (some #{authenticated-user-id} (db.plans/plan-managers plan)))))

(defn user-authorized-to-mutate-user?
  [{:as _authenticated-user authenticated-user-id :id :keys [is-superuser?]}
   {:as _target-user target-user-id :id}]
    (boolean (or is-superuser?
                 (= target-user-id authenticated-user-id))))

;; Authorizers.
;;
;; The following functions throw a 403 exception when specific conditions are
;; not met.

(defn authorize-mutate-old
  "Throw an unauthorized exception if the provided authenticated user is not
  authorized to mutate the provided OLD. The value of mutation should be a
  keyword operation ID, e.g., :create-user-old."
  [mutation old {:as authenticated-user authenticated-user-id :id}]
  (when-not (user-authorized-to-administer-old? authenticated-user old)
    (let [old-admins (db.olds/old-admins old)
          message "Authenticated user is not authorized to mutate this OLD."
          data {:message message
                :authenticated-user-id authenticated-user-id
                :old-slug (:slug old)
                :old-admins old-admins
                :operation-id mutation}]
      (log/warn message data)
      (throw (errors/error-code->ex-info :unauthorized data)))))

(defn authorize-mutate-plan
  "Throw an unauthorized exception if the provided authenticated user is not
  authorized to mutate the provided plan. The value of mutation should be a
  keyword operation ID, e.g., :update-user-plan."
  [mutation plan {:as authenticated-user authenticated-user-id :id}]
  (when-not (user-authorized-to-manage-plan? authenticated-user plan)
    (let [message "Authenticated user is not authorized to mutate this plan."
          data {:message message
                :authenticated-user-id authenticated-user-id
                :plan-id (:id plan)
                :plan-managers (db.plans/plan-managers plan)
                :operation-id mutation}]
      (log/warn message data)
      (throw (errors/error-code->ex-info :unauthorized data)))))
