(ns bbotiscaf.spec.event)

(def event-schema
  [:map
   [:event
    [:map
     [:type string?]
     [:arguments map?]]]])
