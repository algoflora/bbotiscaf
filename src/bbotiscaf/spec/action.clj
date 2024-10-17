(ns bbotiscaf.spec.action
  (:require
    [bbotiscaf.spec.commons :refer [Fail]]))


(def Action
  [:map
   [:method :string]
   [:arguments :map]
   [:timestamp :int]])


(def ActionRequest
  [:map
   [:action Action]])


(def ActionResponseSuccess
  [:map
   [:action Action]
   [:ok [:= true]]
   [:response :any]])


(def ActionResponseFailure
  [:map
   [:action Action]
   [:ok [:= false]]
   [:error Fail]])


(def ActionResponse
  [:or
   ActionResponseSuccess
   ActionResponseFailure])
