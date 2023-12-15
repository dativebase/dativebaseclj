(ns dvb.server.http.utils.pagination-test
  (:require [dvb.server.http.utils.pagination :as sut]
            [clojure.test :refer [deftest is testing]]))

(deftest page-count-works
  ;; Page Count
  ;;           |                 Form Count
  ;;           |   0     1      10      50      100       1000
  ;; ----------+----------------------------------------------
  ;;        1  |   1     1      10      50      100       1000
  ;; IPP   10  |   1     1       1       5       10        100
  ;;       50  |   1     1       1       1        2         20
  ;;      100  |   1     1       1       1        1         10
  ;;     1000  |   1     1       1       1        1          1
  (is (= [1 1 10 50 100 1000]
           [(sut/page-count 0 1)
            (sut/page-count 1 1)
            (sut/page-count 10 1)
            (sut/page-count 50 1)
            (sut/page-count 100 1)
            (sut/page-count 1000 1)]))
  (is (= [1 1 1 5 10 100]
           [(sut/page-count 0 10)
            (sut/page-count 1 10)
            (sut/page-count 10 10)
            (sut/page-count 50 10)
            (sut/page-count 100 10)
            (sut/page-count 1000 10)]))
  (is (=  [1 1 1 1 2 20]
            [(sut/page-count 0 50)
             (sut/page-count 1 50)
             (sut/page-count 10 50)
             (sut/page-count 50 50)
             (sut/page-count 100 50)
             (sut/page-count 1000 50)]))
  (is (= [1 1 1 1 1 10]
           [(sut/page-count 0 100)
            (sut/page-count 1 100)
            (sut/page-count 10 100)
            (sut/page-count 50 100)
            (sut/page-count 100 100)
            (sut/page-count 1000 100)]))
  (is (= [1 1 1 1 1 1]
           [(sut/page-count 0 1000)
            (sut/page-count 1 1000)
            (sut/page-count 10 1000)
            (sut/page-count 50 1000)
            (sut/page-count 100 1000)
            (sut/page-count 1000 1000)])))

(deftest offset-works
  (testing "With large form count: 1000"
    ;; Page 0            Page 1           Page 10           Page    100
    ;; Forms  1000       Forms  1000      Forms  1000       Forms  1000
    ;; IPP  OFFSET       IPP  OFFSET      IPP  OFFSET       IPP  OFFSET
    ;;   1       0       1         1      1        10       1       100
    ;;   5       0       5         5      5        50       5       500
    ;;  10       0       10       10      10      100       10        *
    ;;  50       0       50       50      50      500       50        *
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 1 :form-count 1000})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 5 :form-count 1000})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 10 :form-count 1000})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 50 :form-count 1000})))
    (is (= [1 nil] (sut/offset {:page 1 :items-per-page 1 :form-count 1000})))
    (is (= [5 nil] (sut/offset {:page 1 :items-per-page 5 :form-count 1000})))
    (is (= [10 nil] (sut/offset {:page 1 :items-per-page 10 :form-count 1000})))
    (is (= [50 nil] (sut/offset {:page 1 :items-per-page 50 :form-count 1000})))
    (is (= [10 nil] (sut/offset {:page 10 :items-per-page 1 :form-count 1000})))
    (is (= [50 nil] (sut/offset {:page 10 :items-per-page 5 :form-count 1000})))
    (is (= [100 nil] (sut/offset {:page 10 :items-per-page 10 :form-count 1000})))
    (is (= [500 nil] (sut/offset {:page 10 :items-per-page 50 :form-count 1000})))
    (is (= [100 nil] (sut/offset {:page 100 :items-per-page 1 :form-count 1000})))
    (is (= [500 nil] (sut/offset {:page 100 :items-per-page 5 :form-count 1000})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 10 :form-count 1000})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 50 :form-count 1000}))))
  (testing "With form count: 90"
    ;; Page 0            Page 1           Page 10           Page    100
    ;; Forms    90       Forms    90      Forms    90       Forms    90
    ;; IPP  OFFSET       IPP  OFFSET      IPP  OFFSET       IPP  OFFSET
    ;;   1       0       1         1      1        10       1         *
    ;;   5       0       5         5      5        50       5         *
    ;;  10       0       10       10      10        *       10        *
    ;;  50       0       50       50      50        *       50        *
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 1 :form-count 90})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 5 :form-count 90})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 10 :form-count 90})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 50 :form-count 90})))
    (is (= [1 nil] (sut/offset {:page 1 :items-per-page 1 :form-count 90})))
    (is (= [5 nil] (sut/offset {:page 1 :items-per-page 5 :form-count 90})))
    (is (= [10 nil] (sut/offset {:page 1 :items-per-page 10 :form-count 90})))
    (is (= [50 nil] (sut/offset {:page 1 :items-per-page 50 :form-count 90})))
    (is (= [10 nil] (sut/offset {:page 10 :items-per-page 1 :form-count 90})))
    (is (= [50 nil] (sut/offset {:page 10 :items-per-page 5 :form-count 90})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 10 :form-count 90})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 50 :form-count 90})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 1 :form-count 90})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 5 :form-count 90})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 10 :form-count 90})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 50 :form-count 90}))))
  (testing "With form count: 1"
    ;; Page 0            Page 1           Page 10           Page    100
    ;; Forms     1       Forms     1      Forms     1       Forms     1
    ;; IPP  OFFSET       IPP  OFFSET      IPP  OFFSET       IPP  OFFSET
    ;;   1       0       1         *      1         *       1         *
    ;;   5       0       5         *      5         *       5         *
    ;;  10       0       10        *      10        *       10        *
    ;;  50       0       50        *      50        *       50        *
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 1 :form-count 1})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 5 :form-count 1})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 10 :form-count 1})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 50 :form-count 1})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 1 :items-per-page 1 :form-count 1})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 1 :items-per-page 5 :form-count 1})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 1 :items-per-page 10 :form-count 1})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 1 :items-per-page 50 :form-count 1})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 1 :form-count 1})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 5 :form-count 1})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 10 :form-count 1})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 50 :form-count 1})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 1 :form-count 1})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 5 :form-count 1})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 10 :form-count 1})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 50 :form-count 1}))))
  (testing "With form count: 0"
    ;; Page 0            Page 1           Page 10           Page    100
    ;; Forms     0       Forms     0      Forms     0       Forms     0
    ;; IPP  OFFSET       IPP  OFFSET      IPP  OFFSET       IPP  OFFSET
    ;;   1       0       1         *      1         *       1         *
    ;;   5       0       5         *      5         *       5         *
    ;;  10       0       10        *      10        *       10        *
    ;;  50       0       50        *      50        *       50        *
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 1 :form-count 0})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 5 :form-count 0})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 10 :form-count 0})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 50 :form-count 0})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 1 :items-per-page 1 :form-count 0})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 1 :items-per-page 5 :form-count 0})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 1 :items-per-page 10 :form-count 0})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 1 :items-per-page 50 :form-count 0})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 1 :form-count 0})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 5 :form-count 0})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 10 :form-count 0})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 50 :form-count 0})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 1 :form-count 0})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 5 :form-count 0})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 10 :form-count 0})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 50 :form-count 0}))))
  (testing "With form count: 10"
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 1 :form-count 10})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 5 :form-count 10})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 10 :form-count 10})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 50 :form-count 10})))
    (is (= [1 nil] (sut/offset {:page 1 :items-per-page 1 :form-count 10})))
    (is (= [5 nil] (sut/offset {:page 1 :items-per-page 5 :form-count 10})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 1 :items-per-page 10 :form-count 10})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 1 :items-per-page 50 :form-count 10})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 1 :form-count 10})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 5 :form-count 10})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 10 :form-count 10})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 50 :form-count 10})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 1 :form-count 10})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 5 :form-count 10})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 10 :form-count 10})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 50 :form-count 10}))))
  (testing "With form count: 11"
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 1 :form-count 11})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 5 :form-count 11})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 10 :form-count 11})))
    (is (= [0 nil] (sut/offset {:page 0 :items-per-page 50 :form-count 11})))
    (is (= [1 nil] (sut/offset {:page 1 :items-per-page 1 :form-count 11})))
    (is (= [5 nil] (sut/offset {:page 1 :items-per-page 5 :form-count 11})))
    (is (= [10 nil] (sut/offset {:page 1 :items-per-page 10 :form-count 11})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 1 :items-per-page 50 :form-count 11})))
    (is (= [10 nil] (sut/offset {:page 10 :items-per-page 1 :form-count 11})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 5 :form-count 11})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 10 :form-count 11})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 10 :items-per-page 50 :form-count 11})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 1 :form-count 11})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 5 :form-count 11})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 10 :form-count 11})))
    (is (= [nil :inputs-invalid] (sut/offset {:page 100 :items-per-page 50 :form-count 11})))))
