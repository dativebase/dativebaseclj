(ns dvb.server.http.operations.activate-user
  (:require [dvb.common.openapi.errors :as errors]
            [dvb.common.utils :as utils]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.operations.utils.declojurify :as declojurify]
            [dvb.server.log :as log]))

(defn handle
  [{:as _application :keys [database]}
   {:as _ctx {user-id :user_id
              registration-key :user_registration_key} :path}]
  (let [registration-key (utils/->uuid registration-key)]
    (log/info "Activating a user." {:user-id user-id
                                    :registration-key registration-key})
    (let [user (db.users/get-user database user-id)]
      (when-not user
        (let [data {:entity-type :user
                    :entity-id user-id
                    :operation :activate-user}]
          (log/warn "User not found during activation attempt" data)
          (throw (errors/error-code->ex-info :entity-not-found data))))
      (when-not (= (:registration-key user) registration-key)
        (let [data {:entity-type :user
                    :entity-id user-id
                    :supplied-registration-key registration-key
                    :correct-registration-key (:registration-key user)
                    :operation :activate-user}]
          (log/warn "User activation failed" data)
          (throw (errors/error-code->ex-info :user-activation-failed data))))
      (let [activated-user (db.users/activate-user database user)]
        {:status 200
         :headers {}
         :body (declojurify/user activated-user)}))))
