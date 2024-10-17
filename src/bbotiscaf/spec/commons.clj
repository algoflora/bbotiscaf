(ns bbotiscaf.spec.commons)


(def Fail
  [:map
   [:message :string]
   [:data [:maybe [:map]]]
   [:cause [:maybe [:map]]]
   [:stacktrace [:vector [:fn (fn [ste] (instance? java.lang.StackTraceElement ste))]]]])


(def Regexp
  [:fn (fn [re] (instance? java.util.regex.Pattern re))])
