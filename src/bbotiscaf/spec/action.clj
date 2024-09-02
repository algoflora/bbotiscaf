(ns bbotiscaf.spec.action
  (:require
    [bbotiscaf.spec.commons :refer [Error]]))


(def ActionSchema
  [:map
   [:method :string]
   [:arguments :map]
   [:timestamp :int]])


(def ActionRequest
  [:map
   [:action ActionSchema]])


(def ActionResponseSuccessSchema
  [:map
   [:action ActionSchema]
   [:ok [:= true]]
   [:response :any]])


(def ActionResponseFailureSchema
  [:map
   [:action ActionSchema]
   [:ok [:= false]]
   [:error Error]])


(def ActionResponseSchema
  [:or
   ActionResponseSuccessSchema
   ActionResponseFailureSchema])
