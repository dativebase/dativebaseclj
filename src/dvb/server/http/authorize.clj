(ns dvb.server.http.authorize
  (:require [dvb.common.openapi.errors :as errors]
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
  #{:delete-user ;; NOTE: delete-user operation is currently not supported
    :edit-user
    :new-user})

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

(defn plan-managers [plan]
  (->> plan
       :members
       (filter (comp (partial = :manager) :role))
       (map :id)))

(defn authorize-mutate-user-plan
  "Throw an unauthorized exception if the provided authenticated user is not
  authorized to mutate the provided user-plan relationship to the provided plan.
  The value of mutation should be a keyword operation ID."
  [mutation user-plan plan {:as _authenticated-user
                            authenticated-user-id :id
                            :keys [is-superuser?]}]
  (let [plan-managers (plan-managers plan)
        plan-creator (:created-by plan)]
    (when-not (or is-superuser?
                  (= authenticated-user-id plan-creator)
                  (some #{authenticated-user-id} plan-managers))
      (let [data {:authenticated-user-id authenticated-user-id
                  :user-id (:user-id user-plan)
                  :plan-id (:id plan)
                  :plan-managers plan-managers
                  :plan-creator plan-creator
                  :operation-id mutation}]
        (log/warn "Authenticated user is not authorized to mutate access to this plan."
                  data)
        (throw (errors/error-code->ex-info :unauthorized data))))))

(defn old-admins [old]
  (->> old
       :users
       (filter (comp (partial = :administrator) :role))
       (map :id)))

(defn authorize-mutate-user-old
  "Throw an unauthorized exception if the provided authenticated user is not
  authorized to mutate the provided user-old relationship to the provided OLD.
  The value of mutation should be a keyword operation ID."
  [mutation user-old old {:as _authenticated-user
                          authenticated-user-id :id
                          :keys [is-superuser?]}]
  (let [old-admins (old-admins old)
        old-creator (:created-by old)]
    (when-not (or is-superuser?
                  (= authenticated-user-id old-creator)
                  (some #{authenticated-user-id} old-admins))
      (let [data {:authenticated-user-id authenticated-user-id
                  :user-id (:user-id user-old)
                  :old-slug (:slug old)
                  :old-admins old-admins
                  :old-creator old-creator
                  :operation-id mutation}]
        (log/warn "Authenticated user is not authorized to mutate access to this OLD."
                  data)
        (throw (errors/error-code->ex-info :unauthorized data))))))

(defn user-authorized-to-manage-plan? [user plan]
  (let [{user-id :id :keys [is-superuser?]} user
        plan-managers (plan-managers plan)]
    (boolean (or is-superuser? (some #{user-id} plan-managers)))))

(defn authenticated-user-authorized-to-mutate-user?
  [{:as _authenticated-user authenticated-user-id :id :keys [is-superuser?]}
   {:as _user user-id :id}]
    (boolean (or is-superuser?
                 (= user-id authenticated-user-id))))
