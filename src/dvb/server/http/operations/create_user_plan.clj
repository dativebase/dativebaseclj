(ns dvb.server.http.operations.create-user-plan
  (:require [clojure.string :as str]
            [dvb.common.openapi.errors :as errors]
            [dvb.common.edges :as edges]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.db.users :as db.users]
            [dvb.server.db.user-plans :as db.user-plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (org.postgresql.util PSQLException)))

(defn- create-user-plan [database user-plan-to-create]
  (let [data {:entity-type :user-plan
              :entity-to-create user-plan-to-create}
        throw-500 (fn [e] (throw (errors/error-code->ex-info
                                  :entity-creation-internal-error
                                  data e)))]
    (try
      (db.user-plans/create-user-plan database user-plan-to-create)
      (catch PSQLException e
        (if (str/includes? (ex-message e) "users_plans_unique")
          (throw (errors/error-code->ex-info
                  :users-plans-unique-constraint-violated
                  data))
          (throw-500 e)))
      (catch Exception e (throw-500 e)))))

(defn validate [database {:as user-plan-write :keys [user-id]} plan]
  (let [user (db.users/get-user database user-id)]
    (when-not user
      (let [data {:entity-type :user
                  :entity-id user-id
                  :operation :create-user-plan}]
        (log/warn "User not found" data)
        (throw (errors/error-code->ex-info :entity-not-found data))))
    (when-not plan
      (let [data {:entity-type :plan
                  :entity-id (:id plan)
                  :operation :create-user-plan}]
        (log/warn "Plan not found" data)
        (throw (errors/error-code->ex-info :entity-not-found data))))))

(defn authorize [user-plan-write plan
                 {:as authenticated-user authenticated-user-id :id
                  :keys [is-superuser?]}]
  (let [plan-managers (->> plan
                           :members
                           (filter (comp (partial = :manager) :role))
                           (map :id))
        plan-creator (:created-by plan)]
    (when-not (or is-superuser?
                  (= authenticated-user-id plan-creator)
                  (some #{authenticated-user-id} plan-managers))
      (let [data {:authenticated-user-id authenticated-user-id
                  :user-id (:user-id user-plan-write)
                  :plan-id (:id plan)
                  :plan-managers plan-managers
                  :plan-creator plan-creator
                  :operation-id :create-user-plan}]
        (log/warn "Authenticated user is not authorized to grant access to this plan."
                  data)
        (throw (errors/error-code->ex-info :unauthorized data))))))

(defn handle [{:keys [database]}
              {:as ctx {:as user-plan-write :keys [user-id plan-id]} :request-body}]
  (log/info "Creating a user plan.")
  (authorize/authorize ctx)
  (let [user-plan-write (edges/user-plan-api->clj user-plan-write)
        {:as authenticated-user authenticated-user-id :id :keys [is-superuser?]}
        (utils/security-user ctx)
        plan (db.plans/get-plan-with-members database plan-id)]
    (validate database user-plan-write plan)
    (authorize user-plan-write plan authenticated-user)
    (let [create-fn (partial create-user-plan database)
          response {:status 201
                    :headers {}
                    :body (-> user-plan-write
                              (assoc :created-by authenticated-user-id
                                     :updated-by authenticated-user-id)
                              create-fn
                              edges/user-plan-clj->api)}]
      (log/info "Created a user plan.")
      response)))
