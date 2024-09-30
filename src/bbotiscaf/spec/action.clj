(ns bbotiscaf.spec.action
  (:require
    [bbotiscaf.spec.commons :refer [Error]]))


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
   [:error Error]])


(def ActionResponse
  [:or
   ActionResponseSuccess
   ActionResponseFailure])
