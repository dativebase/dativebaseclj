(ns dvb.server.entities.users-test
  (:require [clojure.test :refer [deftest is]]
            [com.stuartsierra.component :as component]
            [dvb.common.specs.users :as user-specs]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.db.users :as db.users]
            [dvb.server.db.user-olds :as db.user-olds]
            [dvb.server.db.user-plans :as db.user-plans]
            [dvb.server.entities.users :as sut]
            [dvb.server.test-data :as test-data]))

(defn setup []
  (let [database (test-data/db-component)
        superuser* (test-data/gen-user-write {:is-superuser? true
                                              :created-by nil
                                              :updated-by nil})
        {su-id :id :as _superuser}
        (db.users/activate-user database (db.users/create-user database superuser*))
        provenance {:created-by su-id :updated-by su-id}
        user* (test-data/gen-user-write (assoc provenance :is-superuser? false))
        {user-id :id} (db.users/activate-user
                       database
                       (db.users/create-user database user*))
        {plan-id :id} (db.plans/create-plan database {:tier :free
                                                      :created-by user-id
                                                      :updated-by user-id})
        user-plan (db.user-plans/create-user-plan
                   database {:user-id user-id
                             :plan-id plan-id
                             :role :manager
                             :created-by user-id
                             :updated-by user-id})
        {old-slug :slug :as old*} (assoc (test-data/gen-old-write)
                                         :created-by user-id
                                         :updated-by user-id
                                         :plan-id plan-id)
        _old (db.olds/create-old database old*)
        user-old (db.user-olds/create-user-old
                  database {:user-id user-id
                            :old-slug old-slug
                            :role :administrator
                            :created-by user-id
                            :updated-by user-id})
        user (db.users/get-user-with-plans-and-olds database user-id)]
    {:database database
     :user user
     :user-plan user-plan
     :user-old user-old}))

(deftest deactivate-works
  (let [{:keys [database user-old user-plan] {:as _user user-id :id} :user}
        (setup)]
    (try
      (let [ex-data* (try (sut/deactivate database user-id user-id)
                          (catch Exception e (ex-data e)))]
        (is (= :cannot-deactivate-manager (:error-code ex-data*)))
        (db.user-plans/delete-user-plan database user-plan)
        (let [ex-data* (try (sut/deactivate database user-id user-id)
                            (catch Exception e (ex-data e)))]
          (is (= :cannot-deactivate-administrator (:error-code ex-data*))))
        (db.user-olds/delete-user-old database user-old)
        (let [deactivated-user (sut/deactivate database user-id user-id)]
          (is (user-specs/user? deactivated-user))
          (is (= :deactivated (:registration-status deactivated-user)))))
      (finally (component/stop database)))))
