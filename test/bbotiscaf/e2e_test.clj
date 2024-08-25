(ns bbotiscaf.e2e-test
  (:require [clojure.test :refer [deftest testing is]]
            [bbotiscaf.logging]
            [bbotiscaf.impl.e2e :as e2e]
            [bbotiscaf.impl.e2e.dummy :as dum]
            [bbotiscaf.impl.e2e.client :as cl]
            [bbotiscaf.impl.system :as sys]))

(deftest test-1
  (testing "TEST_1"
    (sys/startup!)
    (dum/new :user)
    (cl/send-text :user "Sasha" [])
    (let [msg (dum/get-first-message (:dummy (dum/get-by-key :user)))]
      (is (= "Hi, Sasha!" (:text msg))))))
