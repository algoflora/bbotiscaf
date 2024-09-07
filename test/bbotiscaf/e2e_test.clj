(ns bbotiscaf.e2e-test
  (:require
    [bbotiscaf.impl.e2e.flow :refer [flow]]
    [clojure.test :refer [deftest]]))


(deftest core
  (flow "Main Message" nil
        [:ivan/send-text "Ivan"
         :ivan/check-msg "Hi, Ivan!" '() [[#"Go"] ["Temp"]]
         :mary/send-text "Mary"
         :mary/check-msg "Hi, Mary!" [["Go!"] ["Temp"]]
         :ivan/click-btn #"^Go"
         :mary/click-btn "Go"
         :mary/check-msg "Go, Mary!" '() [["Home"]]
         :ivan/check-msg "Go, Ivan!" [["Home"]]
         :ivan/click-btn "Home"
         :mary/click-btn "Home"
         :ivan/check-msg "Hi, stranger!" [["Go"] ["Temp"]]
         :mary/check-msg #"stranger" '() [["Go"] ["Temp"]]])


  (flow "Temp Message" :main-message
        [:ivan/check-msg "Hi, stranger!" [["Go"] ["Temp"]]
         :mary/check-msg "Hi, stranger!" [["Go"] ["Temp"]]
         :mary/click-btn "Temp"
         :ivan/click-btn "Temp"
         :mary/click-btn "Temp"
         :mary/check-msg 1 "Temp message of Mary" [["✖️"]]
         :ivan/check-msg 1 "Temp message of Ivan" [["✖️"]]
         :mary/check-msg 2 "Temp message of Mary" [["✖️"]]
         :ivan/click-btn 1 "✖️"
         :mary/click-btn 2 "✖️"
         :mary/click-btn 1 "✖️"
         :ivan/check-msg 1 "Hi, stranger!"
         :mary/check-msg 1 "Hi, stranger!"]))


(deftest db
  (flow "Aditional entities" nil 'bbotiscaf.e2e-test.handler/store
        [:ivan/send-text "/start"
         :ivan/check-msg "Hello" [["Save"]]
         :mary/send-text "/start"
         :mary/check-msg "Hello" [["Save"]]
         :ivan/click-btn "Save"
         :mary/click-btn "Save"
         :mary/check-msg "Name saved" [["Reveal"]]
         :ivan/check-msg "Name saved" [["Reveal"]]
         :mary/click-btn "Reveal"
         :ivan/click-btn "Reveal"
         :ivan/check-msg "IVAN"
         :mary/check-msg "MARY"]))
