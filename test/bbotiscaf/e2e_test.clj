(ns bbotiscaf.e2e-test
  (:require [clojure.test :refer [deftest testing is]]
            [bbotiscaf.logging]
            [bbotiscaf.impl.system :as sys]))

(deftest test-1
  (testing "TEST_1"
    (sys/startup!)
    (is (= 1 1))
    (is (= 1 2))
    (is (= 2 2))))
