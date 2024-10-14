(ns bbotiscaf.e2e-test
  (:require
    [bbotiscaf.impl.e2e.flow :refer [flows-out defflow]]))


(defflow :users/main-message
  [:ivan/send-text "Ivan"
   :ivan/check-msg "Hi, Ivan!" #{} [[#"Go"] ["Temp"]]
   :mary/send-text "Mary"
   :mary/check-msg "Hi, Mary!" [["Go!"] ["Temp"]]
   :ivan/click-btn #"^Go"
   :mary/click-btn "Go"
   :mary/check-msg "Go, Mary!" #{} [["Home"]]
   :ivan/check-msg "Go, Ivan!" [["Home"]]
   :ivan/click-btn "Home"
   :mary/click-btn "Home"
   :ivan/check-msg "Hi, stranger!" [["Go"] ["Temp"]]
   :mary/check-msg #"stranger" #{} [["Go"] ["Temp"]]])


(defflow :users/temp-message
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
   :mary/check-msg 1 "Hi, stranger!"])


(defflow :users/additional-entities
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
   :mary/check-msg "MARY"])


(defflow :users/roles
  [:user/send-text "/start"
   :admin/send-text "/start"
   :user/check-msg "Hi"
   :admin/check-msg "Hello, sir"])


(defflow :users/error
  [:user/send-text "/start"
   :user/check-msg "Hello World!" [["Button"]]
   :user/click-btn "Button"
   :user/check-msg "Click Error" [["Error"]]
   :user/click-btn "Error"
   ;; ::call! #(Thread/sleep 1000)
   :user/check-msg 1 "⚠️ Unexpected ERROR! ⚠️" [["To Main Menu"] ["✖️"]]
   :user/click-btn 1 "To Main Menu"
   :user/check-no-temp-messages
   :user/check-msg "Hello World!" [["Button"]]])


(defflow :users/payment
  [:user/send-text "/start"
   :user/check-msg "Give me all your money!" [["Invoice"]]
   :user/click-btn "Invoice"
   :user/check-invoice "Invoice" "All your money!" "XTR" [{:label "Price" :amount 15000} {:label "Discount" :amount -5000}] [["Pay 100 XTR"] ["Dummy button"] ["✖️"]]
   :user/pay-invoice
   :user/approve-pre-checkout-query
   :user/check-and-close-only-temp "Successful payment with payload all-your-money"])


(flows-out Core
           [:users/main-message :users/temp-message])


(flows-out DB
           'bbotiscaf.e2e-test.handler/store
           [:users/additional-entities])


(flows-out Roles
           'bbotiscaf.e2e-test.handler/roled
           [:users/roles])


(flows-out Error
           'bbotiscaf.e2e-test.handler/error
           [:users/error])


(flows-out Payment
           'bbotiscaf.e2e-test.handler/payment
           [:users/payment])
