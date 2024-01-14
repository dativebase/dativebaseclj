(ns dvb.server.db.old-access-requests-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.stuartsierra.component :as component]
            [dvb.common.specs.old-access-requests :as oar-specs]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.old-access-requests :as sut]
            [dvb.server.db.user-olds :as db.user-olds]
            [dvb.server.db.users :as db.users]
            [dvb.server.test-data :as test-data]))

(defn- set-up []
  (let [database (test-data/db-component)
        administrator (db.users/create-user database (test-data/gen-user-write
                                                      {:created-by nil
                                                       :updated-by nil}))
        contributor (db.users/create-user database (test-data/gen-user-write
                                                    {:created-by nil
                                                     :updated-by nil}))
        old (db.olds/create-old database (test-data/gen-old-write
                                          {:created-by (:id administrator)
                                           :updated-by (:id administrator)
                                           :plan-id nil}))]
    (db.user-olds/create-user-old database
                                  {:user-id (:id administrator)
                                   :old-slug (:slug old)
                                   :role :administrator
                                   :created-by (:id administrator)
                                   :updated-by (:id administrator)})
    (db.user-olds/create-user-old database
                                  {:user-id (:id contributor)
                                   :old-slug (:slug old)
                                   :role :contributor
                                   :created-by (:id administrator)
                                   :updated-by (:id administrator)})
    {:administrator administrator
     :contributor contributor
     :requester (db.users/create-user database (test-data/gen-user-write
                                                {:created-by nil
                                                 :updated-by nil}))
     :old old
     :database database}))

(deftest old-access-request-db-fns-work
  (let [{:keys [database requester old]} (set-up)
        {requester-id :id} requester
        {old-slug :slug} old]
    (try
      (testing "We can create several OLD access requests."
        (let [request {:user-id requester-id
                       :old-slug old-slug}
              old-access-request-1 (sut/create-old-access-request database request)
              old-access-request-2 (sut/create-old-access-request database request)
              old-access-request-3 (sut/create-old-access-request database request)]
          (is [:pending :pending :pending]
              (map :status [old-access-request-1
                            old-access-request-2
                            old-access-request-3]))
          (testing "We can fetch all OLD access requests for a given user"
            (let [oars (sut/get-pending-old-access-requests-for-user
                        database requester-id)]
              (is (= 3 (count oars)))
              (is (= #{requester-id} (set (map :user-id oars))))
              (is (oar-specs/old-access-requests? oars))))
          (testing "We can fetch all OLD access requests for a given OLD"
            (let [oars (sut/get-pending-old-access-requests-for-old
                        database old-slug)]
              (is (= 3 (count oars)))
              (is (= #{old-slug} (set (map :old-slug oars))))
              (is (oar-specs/old-access-requests? oars))))
          (let [approved-oar (sut/approve database (:id old-access-request-1))
                rejected-oar (sut/reject database (:id old-access-request-2))
                retracted-oar (sut/retract database (:id old-access-request-3))]
            (testing "We can approve an OLD access request"
              (is (= :approved (:status approved-oar)))
              (is (oar-specs/old-access-request? approved-oar)))
            (testing "We can reject an OLD access request"
              (is (= :rejected (:status rejected-oar)))
              (is (oar-specs/old-access-request? rejected-oar)))
            (testing "We can retract an OLD access request"
                (is (= :retracted (:status retracted-oar)))
                (is (oar-specs/old-access-request? retracted-oar)))
            (testing "Our OLD's OLD access request have the expected statuses"
              (let [oars (sut/get-pending-old-access-requests-for-old
                          database old-slug)]
                (is (oar-specs/old-access-requests? oars))
                (is (empty? oars))
                (is (= :approved
                       (:status (sut/get-old-access-request
                                 database (:id approved-oar)))))
                (is (= :rejected
                       (:status (sut/get-old-access-request
                                 database (:id rejected-oar)))))
                (is (= :retracted
                       (:status (sut/get-old-access-request
                                 database (:id retracted-oar))))))))
          (testing "We can count the OLD access requests"
            (is (int? (sut/count-old-access-requests database))))
          (testing "We can fetch a page of OLD access requests"
            (let [page-of-oars (sut/get-old-access-requests database)]
              (is (oar-specs/old-access-requests? page-of-oars))
              (is (seq page-of-oars))
              (is (< (count page-of-oars) 11))))
          (testing "We can fetch a single OLD access request"
            (let [oar-1 (sut/get-old-access-request database (:id old-access-request-1))]
              (is (oar-specs/old-access-request? oar-1))
              (is (= (assoc old-access-request-1 :status :approved)
                     oar-1))))))
      (finally (component/stop database)))))
