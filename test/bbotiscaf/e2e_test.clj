(ns bbotiscaf.e2e-test
  (:require
   [bbotiscaf.impl.e2e.flow :refer [flow]]
   [clojure.test :refer [deftest]]))


(deftest e2e

  (flow "test-1" {} [:user/send-text "Sasha"
                     :user/check-msg "Hi, Sasha!" '() [[#"^Go"]]
                     :user/click-btn "Go!"
                     :user/check-msg "Go, Sasha!" [["Home"]]
                     :user/click-btn #"Home"
                     :user/check-msg "Hi, stranger!"])


  (flow "test-2" :test-1 [:user/check-msg "Hi, stranger!"
                          :user/click-btn "Go!"])

  (flow "test-2" :test-2 [:user/check-msg "Hi, !"]))
