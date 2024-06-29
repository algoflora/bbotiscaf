(ns bbotiscaf.spec.commons)

(def error-schema
  [:map
   [:message :string]
   [:data [:maybe [:map]]]
   [:cause [:maybe [:map]]]
   [:stacktrace [:vector [:fn #(instance? java.lang.StackTraceElement %)]]]])
