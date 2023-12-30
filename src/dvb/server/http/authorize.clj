(ns dvb.server.http.authorize
  (:require [dvb.common.openapi.errors :as errors]
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
  #{:create-plan
    :create-user
    :delete-user
    :edit-user
    :index-users
    :new-user
    :show-user
    :update-user})

(def superuser-operations
  #{:create-user
    :delete-user
    :edit-user
    :index-users
    :new-user
    :show-user
    :update-user})

(defn authorize [ctx]
  (let [{:keys [roles is-superuser?] user-id :id} (-> ctx :security :user)
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
