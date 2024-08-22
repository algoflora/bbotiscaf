(ns bbotiscaf.spec.action
  (:require [bbotiscaf.spec.commons :refer [error-schema]]))

(def action-schema
  [:map
   [:type :string]
   [:arguments [:map]]])

(def Action-Request
  [:map
   [:action action-schema]])

(def action-response-success-schema
  [:map
   [:action action-schema]
   [:ok [:= true]]
   [:response :any]])

(def action-response-failure-schema
  [:map
   [:action action-schema]
   [:ok [:= false]]
   [:error error-schema]])

(def action-response-schema
  [:or
   action-response-success-schema
   action-response-failure-schema])
