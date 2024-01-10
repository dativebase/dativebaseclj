(ns dvb.server.entities.users
  (:require [clojure.java.jdbc :as jdbc]
            [dvb.server.db.api-keys :as db.api-keys]
            [dvb.server.db.users :as db.users]))

(defn deactivate
  "Safely deactivate a user by setting its registration_status to deactivated. A
  deactivated user still acts as a reference, i.e., a creator of historical
  forms, etc. However, a deactivated user can no longer authenticate to the
  system.
  Note: user deactivation is prohibited under the following conditions.
  - The user is a manager of any active plans.
  - The user is an administrator of any active OLDs.
  Note: user deactivation includes the expiration of any active API keys of the
  deactivated user."
  [db-conn user-id updating-user-id]
  (jdbc/with-db-transaction [tx db-conn]
    (let [{:as user :keys [plans olds]}
          (db.users/get-user-with-plans-and-olds tx user-id)]
      (when-let [plans-managed
                 (seq (for [plan plans :when (= :manager (:role plan))] plan))]
        (let [msg "Refusing to deactivate user that is a manager of a plan."]
          (throw (ex-info msg {:message msg
                               :error-code :cannot-deactivate-manager
                               :user-id user-id
                               :plans-managed (vec plans-managed)}))))
      (when-let [olds-administered
                 (seq (for [old olds :when (= :administrator (:role old))] old))]
        (let [msg "Refusing to deactivate user that is an administrator of an OLD."]
          (throw (ex-info msg {:message msg
                               :error-code :cannot-deactivate-administrator
                               :user-id user-id
                               :olds-administered (vec olds-administered)}))))
      (doseq [api-key (db.api-keys/get-api-keys-for-user tx user)]
        (db.api-keys/delete-api-key tx api-key))
      (db.users/deactivate-user tx {:id user-id
                                    :updated-by updating-user-id}))))
