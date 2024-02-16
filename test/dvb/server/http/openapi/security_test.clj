(ns dvb.server.http.openapi.security-test
  (:require [clojure.test :refer [deftest testing is]]
            [dvb.server.http.openapi.security :as sut]))

;; The tricky thing about security in OpenAPI is that the :security key (whether
;; it be at the root of the spec or top-level in an operation object) is a
;; sequence of optional security challenges. If any one of them pass, then the
;; entire security check passes. Furthermore, if multiple pass, we want the data
;; from all of them. For example, we might want optional authentication on an
;; endpoint such that when authentication succeeds, we have the authenticated
;; user returned in the data of the security challenge.
(deftest run-security-works
  (let [http-component
        {:spec {:components {:security-schemes {:x-api-key {:type :api-key
                                                            :in :header
                                                            :name "X-API-KEY"}
                                                :x-app-id {:type :api-key
                                                           :in :header
                                                           :name "X-APP-ID"}}}}}
        context {:request {:headers {"X-API-KEY" "dog123"
                                     "X-APP-ID" "cat456"}}}
        state (atom 0)
        security-handlers-always-authenticated
        {:api-key (fn [_system _ctx api-key-data]
                    (assoc api-key-data
                           :authenticated? true
                           :handler :always-authenticated))}
        security-handlers-always-authenticated-stateful
        {:api-key (fn [_system _ctx api-key-data]
                    (let [s @state]
                      (swap! state inc)
                      (assoc api-key-data
                             :authenticated? true
                             :handler :always-authenticated-stateful
                             (keyword (str "state-" s)) true)))}
        state-2 (atom 0)
        security-handlers-stateful-alternating
        {:api-key (fn [_system _ctx api-key-data]
                    (let [s @state-2]
                      (swap! state-2 inc)
                      (assoc api-key-data
                             :authenticated? (even? s)
                             :handler :always-authenticated-stateful-alternating
                             (keyword (str "state-" s)) true)))}
        security-handlers-never-authenticated
        {:api-key (fn [_system _ctx api-key-data]
                    (assoc api-key-data
                           :authenticated? false
                           :handler :never-authenticated))}]
    (doseq [[testing-text
             security
             handlers
             {:as expected
              expected-authenticated? :authenticated?}]
            [["One security option and it always authenticates."
              [{:x-api-key [] :x-app-id []}]
              security-handlers-always-authenticated
              {:authenticated? true
               :handler :always-authenticated}]
             ["One security option and it never authenticates."
              [{:x-api-key [] :x-app-id []}]
              security-handlers-never-authenticated
              {:authenticated? false}]
             ["One security option and it always authenticates and uniquely adds data."
              [{:x-api-key [] :x-app-id []}]
              security-handlers-always-authenticated-stateful
              {:authenticated? true
               :handler :always-authenticated-stateful
               :state-0 true}]
             ["Two security options and they both always authenticate and uniquely add data: all data is retained."
              [{:x-api-key [] :x-app-id []}
               {:x-api-key [] :x-app-id []}]
              security-handlers-always-authenticated-stateful
              {:authenticated? true
               :handler :always-authenticated-stateful
               :state-1 true
               :state-2 true}]
             ["Two security options: first authenticates, second doesn't. Result is authenticated. Only data from successful authentication is retained."
              [{:x-api-key [] :x-app-id []}
               {:x-api-key [] :x-app-id []}]
              security-handlers-stateful-alternating
              {:authenticated? true
               :handler :always-authenticated-stateful-alternating
               :state-0 true}]
             ["Three security options: first authenticates, second doesn't, third does. Result is authenticated. Only data from successful authentication is retained."
              [{:x-api-key [] :x-app-id []}
               {:x-api-key [] :x-app-id []}
               {:x-api-key [] :x-app-id []}]
              security-handlers-stateful-alternating
              {:authenticated? true
               :handler :always-authenticated-stateful-alternating
               :state-2 true
               :state-4 true}]
             ["Two security options: first is empty (and thus vacuously authenticates), second also authenticates. Result is authenticated. Only data comes from successful authentication and is retained."
              [{}
               {:x-api-key [] :x-app-id []}]
              security-handlers-always-authenticated
              {:authenticated? true
               :handler :always-authenticated}]
             ["Two security options: first is empty (and thus vacuously authenticates), second doesn't authenticate. Result is authenticated. No data."
              [{}
               {:x-api-key [] :x-app-id []}]
              security-handlers-never-authenticated
              {:authenticated? true}]]]

      (testing testing-text
        (let [{actual-authenticated? :authenticated? :as actual}
              (try (:security (sut/run-security
                               (-> http-component
                                   (assoc-in [:spec :security] security)
                                   (assoc :security-handlers handlers))
                               context))
                   (catch Exception e {:authenticated? false
                                       :data (ex-data e)}))]
          (is (= expected-authenticated? actual-authenticated?))
          (is (= expected
                 (select-keys actual (keys expected)))))))))
