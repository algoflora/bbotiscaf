(ns bbotiscaf.e2e-test
  (:require [bbotiscaf.impl.e2e.flow :refer [flow]]))


(def data
  (flow "test-1" {} [:user/send-text "Sasha"
                     :user/check-msg "Hi, Sasha!" '() [["Go!"]]
                     :user/click-btn "Go!"
                     :user/check-msg "Go, Sasha!" [["Home"]]
                     :user/click-btn #"Home"
                     :user/check-msg "Hi, stranger!"
                     ]))


(clojure.pprint/pprint data)
