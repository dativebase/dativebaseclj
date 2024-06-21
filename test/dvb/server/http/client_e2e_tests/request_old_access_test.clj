(ns dvb.server.http.client-e2e-tests.request-old-access-test
  (:require [clojure.test :refer [deftest testing is]]
            [dvb.server.http.client-e2e-tests.utils :as u]
            [com.stuartsierra.component :as component]
            [dvb.client.core :as client]
            [dvb.common.specs.old-access-requests :as old-access-request-specs]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.users :as db.users]
            [dvb.server.db.user-olds :as db.user-olds]
            [dvb.server.test-data :as test-data]))

(defn setup
  "Create two non-superusers and an OLD, with the first user being an admin of
  the OLD."
  [database]
  (let [{su-pwd :password :as superuser*}
        (test-data/gen-user-write {:is-superuser? true
                                   :created-by nil
                                   :updated-by nil})
        {su-id :id :as superuser} (db.users/activate-user
                                   database
                                   (db.users/create-user database superuser*))
        provenance {:created-by su-id :updated-by su-id}
        {u-pwd :password :as user*}
        (test-data/gen-user-write (assoc provenance :is-superuser? false))
        user (db.users/activate-user
              database
              (db.users/create-user database user*))
        {u2-pwd :password :as user-2*}
        (test-data/gen-user-write (assoc provenance :is-superuser? false))
        user-2 (db.users/activate-user
                database
                (db.users/create-user database user-2*))
        {old-slug :slug :as old*} (assoc (test-data/gen-old-write provenance)
                                         :plan-id nil)
        old (db.olds/create-old database old*)]
    (db.user-olds/create-user-old
     database (merge provenance {:user-id (:id user)
                                 :old-slug old-slug
                                 :role :administrator}))
    {:user-1 user
     :user-1-password u-pwd
     :user-2 user-2
     :user-2-password u2-pwd
     :superuser superuser
     :superuser-password su-pwd
     :old old}))

(deftest request-old-access-works-end-to-end
  (let [{:keys [database] :as system} (component/start (u/new-system))]
    (try
      (let [{:keys [user-1-password user-2-password]
             {:as _user-1 user-1-email :email user-1-id :id} :user-1
             {:as _user-2 user-2-email :email user-2-id :id} :user-2
             {:as _old old-slug :slug} :old} (setup database)
            user-1-client (client/authenticate-client
                           (client/make-client :local-test)
                           user-1-email user-1-password)
            user-2-client (client/authenticate-client
                           (client/make-client :local-test)
                           user-2-email user-2-password)]
        (testing "We can not create an OLD access request for a non-existent OLD."
          (let [{:keys [status] error :body}
                (client/create-old-access-request
                 user-2-client {:user-id user-2-id
                                :old-slug (str (random-uuid))})]
            (is (= 404 status))
            (is (= "entity-not-found" (u/first-error-code error)))
            (is (= "old-access-request-on-nonexistent-old"
                   (u/first-data-error-code error)))))
        (testing "We can not create an OLD access request for a non-existent user."
          (let [{:keys [status] error :body}
                (client/create-old-access-request
                 user-2-client {:user-id (random-uuid)
                                :old-slug old-slug})]
            (is (= 404 status))
            (is (= "entity-not-found" (u/first-error-code error)))
            (is (= "old-access-request-on-nonexistent-user"
                   (u/first-data-error-code error)))))
        (testing "We can not create an OLD access request for the admin of the OLD."
          (let [{:keys [status] error :body}
                (client/create-old-access-request
                 user-2-client {:user-id user-1-id
                                :old-slug old-slug})]
            (is (= 400 status))
            (is (= "redundant-request" (u/first-error-code error)))
            (is (= "old-access-request-redundant"
                   (u/first-data-error-code error)))))
        (testing "We can create an OLD access request for the second user."
          (let [{:keys [status] {:as created-oar oar-id :id} :body}
                (client/create-old-access-request
                 user-2-client {:user-id user-2-id
                                :old-slug old-slug})]
            (is (= 201 status))
            (is (old-access-request-specs/old-access-request? created-oar))
            (testing "We can fetch the newly created OLD access request."
              (let [{:keys [status] fetched-oar :body}
                    (client/show-old-access-request user-2-client oar-id)]
                (is (= 200 status))
                (is (old-access-request-specs/old-access-request? fetched-oar))
                (is (= created-oar fetched-oar))))
            (testing "We cannot create a redundant OLD access request."
              (let [{:keys [status] error :body}
                    (client/create-old-access-request user-2-client
                                                      {:user-id user-2-id
                                                       :old-slug old-slug})]
                (is (= 400 status))
                (is (= "redundant-request" (u/first-error-code error)))
                (is (= "old-access-request-redundant"
                       (u/first-data-error-code error)))))
            (testing "The admin cannot retract the second user's OLD access request."
              (let [{:keys [status] error :body}
                    (client/retract-old-access-request user-1-client oar-id)]
                (is (= 403 status))
                (is (= "unauthorized" (u/first-error-code error)))
                (is (= "only-target-can-retract-access-request"
                       (u/first-data-error-code error)))))
            (testing "The second user can retract their own OLD access request."
              (let [{:keys [status] retracted-oar :body}
                    (client/retract-old-access-request user-2-client oar-id)]
                (is (= 200 status))
                (is (old-access-request-specs/old-access-request? retracted-oar))
                (is (= (assoc created-oar :status :retracted) retracted-oar))))))
        (testing "After retracting the first one, we can create another OLD access request for the second user."
          (let [{:keys [status] {:as created-oar oar-id :id} :body}
                (client/create-old-access-request
                 user-2-client {:user-id user-2-id
                                :old-slug old-slug})]
            (is (= 201 status))
            (is (old-access-request-specs/old-access-request? created-oar))
            (testing "The target user cannot reject their own OLD access request."
              (let [{:keys [status] error :body}
                    (client/reject-old-access-request user-2-client oar-id)]
                (is (= 403 status))
                (is (= "unauthorized" (u/first-error-code error)))
                (is (= "only-admins-can-reject-old-access-requests"
                       (u/first-data-error-code error)))))
            (testing "The admin user can reject the second user's OLD access request."
              (let [{:keys [status] rejected-oar :body}
                    (client/reject-old-access-request user-1-client oar-id)]
                (is (= 200 status))
                (is (old-access-request-specs/old-access-request? rejected-oar))
                (is (= (assoc created-oar :status :rejected) rejected-oar))))))
        (testing "After rejecting the second one, we can create yet another OLD access request for the second user."
          (let [{:keys [status] {:as created-oar oar-id :id} :body}
                (client/create-old-access-request
                 user-2-client {:user-id user-2-id
                                :old-slug old-slug})]
            (is (= 201 status))
            (is (old-access-request-specs/old-access-request? created-oar))
            (testing "The non-member of the OLD cannot view all access requests for the target OLD."
              (let [{:keys [status] error :body}
                    (client/access-requests-for-old user-2-client old-slug)]
                (is (= 403 status))
                (is (= "unauthorized" (u/first-error-code error)))))
            (testing "The admin of the OLD can view all access requests for that OLD."
              (let [{:keys [status] requests :body}
                    (client/access-requests-for-old user-1-client old-slug)]
                (is (= 200 status))
                (is (old-access-request-specs/old-access-requests? requests))
                (is (= [{:user-id user-2-id
                         :old-slug old-slug
                         :status :pending}]
                       (for [oar requests]
                         (select-keys oar [:user-id :old-slug :status]))))))
            (testing "The target user cannot approve their own OLD access request."
              (let [{:keys [status] error :body}
                    (client/approve-old-access-request user-2-client oar-id)]
                (is (= 403 status))
                (is (= "unauthorized" (u/first-error-code error)))
                (is (= "only-admins-can-approve-old-access-requests"
                       (u/first-data-error-code error)))))
            (testing "The admin user can approve the second user's OLD access request."
              (let [{:keys [status] approved-oar :body}
                    (client/approve-old-access-request user-1-client oar-id)]
                (is (= 200 status))
                (is (old-access-request-specs/old-access-request? approved-oar))
                (is (= (assoc created-oar :status :approved) approved-oar))
                (let [{old-with-users :body} (client/show-old user-2-client old-slug
                                                              {:include-users? true})]
                  (is (= [{:role :viewer}]
                         (for [u (:users old-with-users)
                               :when (= user-2-id (:id u))]
                           (select-keys u [:role]))))))))))
      (finally (component/stop system)))))
