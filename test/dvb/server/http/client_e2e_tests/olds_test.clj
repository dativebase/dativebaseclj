(ns dvb.server.http.client-e2e-tests.olds-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [dvb.client.core :as client]
            [dvb.common.specs.olds :as old-specs]
            [dvb.server.http.client-e2e-tests.utils :as u]
            [dvb.server.test-data :as test-data]))

(deftest olds-endpoints-work-end-to-end
  (let [{:keys [database] :as system} (component/start (u/new-system))]
    (try
      (let [{:keys [user user-password]} (u/setup database)
            user-email (:email user)
            client (client/authenticate-client
                    (client/make-client :local-test)
                    user-email user-password)]
        (testing "We can create a new OLD."
          (let [{:keys [status] {:as created-old old-slug :slug} :body}
                (client/create-old client (test-data/gen-old-write))]
            (is (= 201 status))
            (is (nil? (:destroyed-at created-old)))
            (is (old-specs/old? created-old))
            (testing "We cannot create a new OLD with the same slug that was just used."
              (let [{:keys [status] error :body}
                    (client/create-old client (test-data/gen-old-write
                                               {:slug old-slug}))]
                (is (= 400 status))
                (is (= "unique-slug-constraint-violated"
                       (-> error :errors first :error-code)))))
            (testing "We can fetch the newly created OLD."
              (let [{:keys [status] fetched-old :body}
                    (client/show-old client old-slug {:include-users? true})]
                (is (= 200 status))
                (is (old-specs/old? fetched-old))
                (is (= created-old fetched-old))))
            (testing "We can update the name of the newly created OLD."
              (let [{:keys [status] updated-old :body}
                    (client/update-old client old-slug {:name "Funions"})]
                (is (= 200 status))
                (is (old-specs/old? updated-old))
                (is (= (-> created-old
                           (assoc :name "Funions")
                           (dissoc :updated-at :updated-by :users))
                       (dissoc updated-old :updated-at :updated-by)))
                (testing "We can query all of the OLDs in the system"
                  (let [{:keys [status] {first-page-of-olds :data
                                         :keys [meta]} :body}
                        (client/index-olds client)]
                    (is (= 200 status))
                    (is (old-specs/olds? first-page-of-olds))
                    (is (<= (count first-page-of-olds) (:items-per-page meta)))
                    (is (zero? (:page meta)))))
                (testing "We can delete the newly created OLD."
                  (let [{:keys [status] deleted-old :body}
                        (client/delete-old client old-slug)]
                    (is (= 200 status))
                    (is (old-specs/old? deleted-old))
                    (is (= (-> updated-old
                               (dissoc :updated-at :destroyed-at))
                           (dissoc deleted-old :updated-at :destroyed-at))))))))))
      (finally (component/stop system)))))
