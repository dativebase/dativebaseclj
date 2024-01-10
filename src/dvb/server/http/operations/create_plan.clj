(ns dvb.server.http.operations.create-plan
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [dvb.common.openapi.errors :as errors]
            [dvb.common.edges :as edges]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.db.users :as db.users]
            [dvb.server.db.user-plans :as db.user-plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (org.postgresql.util PSQLException)))

(defn- create-plan-in-db
  [database plan-to-create]
  (let [data {:entity-type :plan
              :entity-to-create plan-to-create}
        throw-500 (fn [e] (throw (errors/error-code->ex-info
                                  :entity-creation-internal-error
                                  data e)))]
    (try
      (db.plans/create-plan database plan-to-create)
      (catch PSQLException e
        (if (str/includes? (ex-message e) "plans_email_key")
          (throw (errors/error-code->ex-info
                  :unique-email-constraint-violated
                  data))
          (throw-500 e)))
      (catch Exception e (throw-500 e)))))

;; The maximum number of plans for which a single user may be a manager.
;; If we make this greater than 1, then a new user can create multiple free
;; plans, which does not incentivize signing up for a subscriber-level plan.
(def max-managed-plan-count 1)

(defn- authorize [user-with-plans]
  (when-not (:is-superuser? user-with-plans)
    (let [managed-plans (->> user-with-plans
                             :plans
                             (filter (comp (partial = :manager) :role)))
          managed-plans-count (count managed-plans)]
      (when (>= managed-plans-count max-managed-plan-count)
        (let [message "Attempted to exceed maximum number of allowed managed plans."
              data {:message message
                    :user-id (:id user-with-plans)
                    :managed-plans-count managed-plans-count
                    :managed-plans (mapv :id managed-plans)}]
          (log/warn message data)
          (throw (errors/error-code->ex-info :unauthorized)))))))

(defn- validate [plan-write authenticated-user-id]
  (when-not (= :free (:tier plan-write))
    (let [message "Attempted to create non-free plan via API call."
          data {:message message
                :authenticated-user-id authenticated-user-id
                :tier (:tier plan-write)
                :error-code :non-free-plan-creation-attempt}]
      (log/warn message data)
      (throw (errors/error-code->ex-info :prohibited-plan-tier)))))

(defn handle
  "Create a new plan. Plan creation entails making the creating user a manager
  of the plan by creating a row in the users_plans table."
  [{:keys [database]} {:as ctx plan-write :request-body}]
  (log/info "Creating a plan.")
  (authorize/authorize ctx)
  (let [plan-write (edges/plan-api->clj plan-write)
        authenticated-user-id (utils/security-user-id ctx)]
    (jdbc/with-db-transaction [tx database]
      (let [user-with-plans (db.users/get-user-with-plans
                             tx authenticated-user-id)]
        (authorize user-with-plans)
        (validate plan-write authenticated-user-id)
        (let [plan (create-plan-in-db tx (assoc plan-write
                                                :created-by authenticated-user-id
                                                :updated-by authenticated-user-id))]
          (db.user-plans/create-user-plan
           tx {:user-id authenticated-user-id
               :plan-id (:id plan)
               :role :manager
               :created-by authenticated-user-id
               :updated-by authenticated-user-id})
          (let [response {:status 201
                          :headers {}
                          :body (edges/plan-clj->api (db.plans/get-plan-with-members
                                                      tx (:id plan)))}]
            (log/info "Created a plan.")
            response))))))
