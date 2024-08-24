(ns bbotiscaf.spec.commons)

(def Error
  [:map
   [:message :string]
   [:data [:maybe [:map]]]
   [:cause [:maybe [:map]]]
   [:stacktrace [:vector [:fn #(instance? java.lang.StackTraceElement %)]]]])
