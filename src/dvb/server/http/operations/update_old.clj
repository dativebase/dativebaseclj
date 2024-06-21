(ns dvb.server.http.operations.update-old
  (:require [dvb.common.edges.olds :as old-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]
            [dvb.server.http.operations.utils :as utils]))

(defn authorize-old-plan
  "Authorize the authenticated user to update this OLD to be paid for under the
  referenced plan."
  [database old-update authenticated-user]
  (when-let [plan-id (:plan-id old-update)]
    (let [plan (db.plans/get-plan-with-members database plan-id)]
      (when-not (authorize/user-authorized-to-manage-plan? authenticated-user plan)
        (let [message "Authenticated user is not authorized to pay for this OLD under this plan."
              data {:message message
                    :authenticated-user-id (:id authenticated-user)
                    :plan-id (:id plan)
                    :plan-members (:members plan)
                    :operation-id :update-old}]
          (log/warn message data)
          (throw (errors/error-code->ex-info :unauthorized data)))))))

(defn validate [old-update existing-old old-slug]
  (when-not existing-old
    (throw (errors/error-code->ex-info
            :entity-not-found
            {:entity-type :old
             :old-slug old-slug
             :operation :update-old})))
  (when (= old-update (select-keys existing-old (keys old-update)))
    (throw (errors/error-code->ex-info
            :no-changes-in-update
            {:request-payload old-update
             :entity-type :old
             :old-slug old-slug
             :operation :update-old}))))

(defn handle [{:keys [database]}
              {:as ctx old-update :request-body {old-slug :old_slug} :path}]
  (let [{:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        log-ctx {:authenticated-user-id authenticated-user-id
                 :old-slug old-slug}]
    (log/info "Updating an OLD." log-ctx)
    (authorize/authorize ctx)
    (authorize-old-plan database old-update authenticated-user)
    (let [old-update (old-edges/api->clj old-update)
          existing-old (db.olds/get-old-with-users database old-slug)
          update-fn (partial db.olds/update-old database)]
      (validate old-update existing-old old-slug)
      (authorize/authorize-mutate-old
       :update-old existing-old authenticated-user)
      (try
        (let [updated-old (-> existing-old
                              (merge old-update
                                     {:updated-by authenticated-user-id})
                              (update :plan-id identity)
                              update-fn
                              old-edges/clj->api)]
          (log/info "Updated an OLD." log-ctx)
          {:status 200
           :headers {}
           :body updated-old})
        (catch Exception e
          (throw (errors/error-code->ex-info
                  :entity-update-internal-error
                  {:entity-type :old
                   :entity-to-update old-update}
                  e)))))))
