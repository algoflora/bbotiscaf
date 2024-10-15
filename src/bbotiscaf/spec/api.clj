(ns bbotiscaf.spec.api
  (:require
    [bbotiscaf.spec.core :as spec]
    [bbotiscaf.spec.model :as spec.mdl]
    [bbotiscaf.spec.telegram :as spec.tg]))


(def TextArgs
  [:cat
   :keyword
   spec.mdl/User
   :string
   [:? [:set spec.tg/MessageEntity]]
   [:? spec/Buttons]
   [:? :int]
   [:* :keyword]])


(def InvoiceData
  [:map
   {:closed true}
   [:title [:string {:min 1 :max 32}]]
   [:description [:string {:min  1 :max 255}]]
   [:payload [:string {:min 1 :max 128}]]
   [:provider_token :string]
   [:currency [:string {:min 3 :max 3}]]
   [:prices [:vector [:map {:closed true}
                      [:label :string]
                      [:amount :int]]]]])


(def InvoiceArgs
  [:cat
   :keyword
   spec.mdl/User
   :string
   InvoiceData])
