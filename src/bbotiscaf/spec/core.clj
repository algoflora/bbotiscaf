(ns bbotiscaf.spec.core
  (:require
    [bbotiscaf.button :as b]
    [bbotiscaf.spec.action :as spec.act]
    [bbotiscaf.spec.telegram :as spec.tg]))


(def Request
  [:or
   spec.tg/Update
   spec.act/ActionRequest])


(def UserOpts
  [:map
   {:closed true}
   [:lambda-name :string]
   [:cluster :string]
   [:lambda-memory-size {:optional true} [:int {:min 128 :max 10240}]]
   [:lambda-timeout {:optional true} [:int {:min 1 :max 900}]]
   [:lambda-env-vars {:optional true} [:vector {:min 0} :string]]
   [:datalevin-version {:optional true} :string]
   [:tfstate-bucket {:optional true} :string]])


(def Buttons
  [:vector
   [:maybe [:vector
            [:maybe [:fn (fn [btn] (instance? b/KeyboardButton btn))]]]]])
