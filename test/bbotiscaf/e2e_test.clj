(ns bbotiscaf.e2e-test
  (:require
    [bbotiscaf.impl.e2e :as e2e]
    [bbotiscaf.impl.e2e.client :as cl]
    [bbotiscaf.impl.e2e.dummy :as dum]
    [bbotiscaf.impl.system :as sys]
    [bbotiscaf.logging]
    [clojure.test :refer [deftest testing is]]))


(deftest test-1
  (testing "TEST_1"
    (sys/startup!)
    (dum/new :user)
    (cl/send-text :user "Sasha" [])
    (let [msg (dum/get-first-message (:dummy (dum/get-by-key :user)))]
      (is (= "Hi, Sasha!" (:text msg)))
      (cl/click-btn :user msg #"Go"))
    (let [msg (dum/get-first-message (:dummy (dum/get-by-key :user)))]
      (is (= "Go, Sasha!" (:text msg)))
      (cl/click-btn :user msg #"Home"))
    (let [msg (dum/get-first-message (:dummy (dum/get-by-key :user)))]
      (is (= "Hi, stranger!" (:text msg))))))

