(ns bbotiscaf.spec.commons)

(def Error
  [:map
   [:message :string]
   [:data [:maybe [:map]]]
   [:cause [:maybe [:map]]]
   [:stacktrace [:vector [:fn (fn [ste] (instance? java.lang.StackTraceElement ste))]]]])
