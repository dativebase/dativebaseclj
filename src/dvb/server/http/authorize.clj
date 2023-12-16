(ns dvb.server.http.authorize
  (:require [dvb.common.openapi.errors :as errors]
            [taoensso.timbre :as log]))

(def operation-roles
  "Map from operation IDs to collections of roles that are authorized to execute
   the operation. If the roles collection is `[:*]`, then all roles are
   authorized for the operation."
  {:index-forms [:*]
   :create-form [:administrator :contributor]
   :delete-form [:administrator :contributor]
   :show-form [:*]
   :show-user [:*]
   :update-form [:administrator :contributor]})

(def old-independent-operations
  #{:show-user})

(defn authorize [ctx]
  (let [roles (-> ctx :security :user :roles)
        old-slug (-> ctx :path :old_slug)
        operation-id (-> ctx :operation :operation-id)
        old-independent-operation? (boolean (operation-id old-independent-operations))
        role-for-old (get roles old-slug)]
    (when (and (not old-independent-operation?)
               (not role-for-old))
      (log/warn "Authenticated user has no roles in the target OLD."
                {:user (-> ctx :security :api-key :user)
                 :old old-slug})
      (throw (errors/error-code->ex-info :unauthorized)))
    (let [current-operation-roles (operation-id operation-roles)]
      (when-not (or (= [:*] current-operation-roles)
                    (some #{role-for-old} current-operation-roles))
        (log/warn "Authenticated user lacks the role required for the target operation."
                  {:user (-> ctx :security :api-key :user)
                   :old old-slug
                   :operation-id operation-id
                   :operation-roles current-operation-roles})
        (throw (errors/error-code->ex-info :unauthorized))))))
