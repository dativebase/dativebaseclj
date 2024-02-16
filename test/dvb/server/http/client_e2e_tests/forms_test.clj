(ns dvb.server.http.client-e2e-tests.forms-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [dvb.client.core :as client]
            [dvb.common.specs.forms :as form-specs]
            [dvb.server.http.client-e2e-tests.utils :as u]
            [dvb.server.test-data :as test-data]))

(deftest forms-endpoints-work-end-to-end
  (let [{:keys [database] :as system} (component/start (u/new-system))]
    (try
      (let [{:keys [user-password]
             {:as user user-id :id} :user
             {:as _old :keys [slug]} :old} (u/setup database)
            user-email (:email user)
            client (client/authenticate-client
                    (client/make-client :local-test)
                    user-email user-password)]
        (testing "Forms can be created via API request."
          (let [form-create-responses
                (mapv (fn [i]
                        (client/create-form
                         client
                         slug
                         (assoc (test-data/gen-form-write)
                                :transcription
                                (str "transcription-" i))))
                      (range 20))]
            (doseq [{:keys [status] created-form :body} form-create-responses]
              (is (= 201 status))
              (is (form-specs/form? created-form))
              (is (= user-id (:created-by created-form)))
              (is (= user-id (:updated-by created-form)))
              (is (= (:updated-at created-form)
                     (:created-at created-form)))
              (is (nil? (:destroyed-at created-form))))
            (let [[form-1 form-2 form-3 :as _created-forms]
                  (map :body form-create-responses)]
              (testing "We can update a form."
                (let [form-update (update form-1 :transcription str " updated")
                      {:as _updated-form-response :keys [status] updated-form :body}
                      (client/update-form
                       client
                       slug
                       (:id form-1)
                       form-update)]
                  (is (= 200 status))
                  (is (not= (:updated-at updated-form)
                            (:created-at updated-form)))
                  (is (= (dissoc form-update :updated-at)
                         (-> updated-form
                             (select-keys (keys form-update))
                             (dissoc :updated-at))))))
              (testing "We can delete a form."
                (let [{:as _deleted-form-response :keys [status] deleted-form :body}
                      (client/delete-form client slug (:id form-2))]
                  (is (= 200 status))
                  (is (not= (:updated-at deleted-form)
                            (:created-at deleted-form)))
                  (is (= (dissoc form-2 :updated-at :destroyed-at)
                         (dissoc deleted-form :updated-at :destroyed-at)))))
              (testing "We can read a form."
                (let [{:as _show-form-response :keys [status] shown-form :body}
                      (client/show-form client slug (:id form-3))]
                  (is (= 200 status))
                  (is (= form-3 shown-form))))
              (testing "We get nothing when attempting to read a deleted form."
                (let [{:keys [status] error :body}
                      (client/show-form client slug (:id form-2))]
                  (is (= 404 status))
                  (is (= "entity-not-found" (u/first-error-code error)))))
              (testing "We can index (i.e., read multiple) forms."
                (let [{:keys [status] {forms :data :keys [meta]} :body}
                      (client/index-forms client slug)]
                  (is (= 200 status))
                  (is (= {:count 19
                          :page 0
                          :items-per-page 10}
                         meta))
                  (is (form-specs/forms? forms))
                  (is (= 10 (count forms)))))
              (testing "We read the second page of forms."
                (let [{:keys [status] {forms-page-2 :data :keys [meta]} :body}
                      (client/index-forms client slug {:page 1})]
                  (is (= 200 status))
                  (is (= {:count 19
                          :page 1
                          :items-per-page 10}
                         meta))
                  (is (form-specs/forms? forms-page-2))
                  (is (= 9 (count forms-page-2)))))))))
      (finally (component/stop system)))))
