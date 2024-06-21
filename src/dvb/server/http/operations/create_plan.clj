(ns dvb.server.http.operations.create-plan
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [dvb.common.openapi.errors :as errors]
            [dvb.common.edges.plans :as plan-edges]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.db.users :as db.users]
            [dvb.server.db.user-plans :as db.user-plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log]
            [dvb.server.system.clock :as clock]
            [java-time.api :as jt])
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

;; 24h between plan creations for a single IP
(def minimum-seconds-between-plan-creation (* 24 60 60))

(defn- rate-limit [database clock remote-addr]
  (when-let [other-plan (db.plans/most-recent-plan-created-by-ip-address
                         database remote-addr)]
    (let [now (clock/now clock)
          other-plan-created-at (:created-at other-plan)
          seconds-since-last-creation
          (jt/as (jt/duration other-plan-created-at now) :seconds)]
      (when (< seconds-since-last-creation minimum-seconds-between-plan-creation)
        (let [message "Plan creation blocked by rate limiting."
              data {:message message
                    :ip-address remote-addr
                    :last-plan-id-created-by-this-ip (:id other-plan)
                    :last-plan-ip-created-by-this-ip (:created-by-ip-address other-plan)
                    :last-plan-created-at (:created-at other-plan)
                    :seconds-since-last-creation seconds-since-last-creation
                    :retry-after minimum-seconds-between-plan-creation
                    :minimum-seconds-between-plan-creation minimum-seconds-between-plan-creation
                    :operation-id :create-plan}]
          (log/warn message data)
          (let [error-code :too-many-requests
                response-429 (assoc-in
                              (errors/error-code->response error-code)
                              [:body :errors 0 :retry-after]
                              (* minimum-seconds-between-plan-creation 60 60))]
            (throw (ex-info (errors/error-code->message error-code)
                            response-429))))))))

(defn handle
  "Create a new plan. Plan creation entails making the creating user a manager
  of the plan by creating a row in the users_plans table."
  [{:keys [database clock]} {:as ctx plan-write :request-body :keys [request]}]
  (log/info "Creating a plan.")
  (authorize/authorize ctx)
  (let [plan-write (plan-edges/api->clj plan-write)
        authenticated-user-id (utils/security-user-id ctx)]
    (jdbc/with-db-transaction [tx database]
      (let [user-with-plans (db.users/get-user-with-plans
                             tx authenticated-user-id)
            remote-addr (utils/remote-addr request)]
        (authorize user-with-plans)
        (validate plan-write authenticated-user-id)
        (rate-limit database clock remote-addr)
        (let [plan (create-plan-in-db
                    tx (assoc plan-write
                              :created-by authenticated-user-id
                              :updated-by authenticated-user-id
                              :created-by-ip-address (or remote-addr "unknown")))]
          (db.user-plans/create-user-plan
           tx {:user-id authenticated-user-id
               :plan-id (:id plan)
               :role :manager
               :created-by authenticated-user-id
               :updated-by authenticated-user-id})
          (let [response {:status 201
                          :headers {}
                          :body (plan-edges/clj->api (db.plans/get-plan-with-members
                                                      tx (:id plan)))}]
            (log/info "Created a plan.")
            response))))))
