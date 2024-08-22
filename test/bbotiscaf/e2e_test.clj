(ns bbotiscaf.e2e-test
  (:require [clojure.test :refer [deftest testing is]]
            [bbotiscaf.logging]
            [bbotiscaf.impl.e2e :as e2e]
            [bbotiscaf.impl.system :as sys]))

(deftest test-1
  (testing "TEST_1"
    (sys/startup!)
    (e2e/send-update {:message {:message_id 1
                                :from {:id 1
                                       :is_bot true
                                       :first_name "Sasha"
                                       :username "laniakealandscape"}
                                :chat {:id 1
                                       :type "private"}
                                :date (System/currentTimeMillis)
                                :text "Hello, Bbotiscaf!"}})
    (is (= 1 2))))
