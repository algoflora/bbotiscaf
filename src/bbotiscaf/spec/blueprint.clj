(ns bbotiscaf.spec.blueprint
  (:require
    [bbotiscaf.spec.commons :refer [Regexp]]
    [bbotiscaf.spec.telegram :as spec.tg]))


(defn is-ns-kw?
  [x]
  (and (keyword? x) (some? (namespace x))))


(def SendTextBlueprintEntryArgs
  [:cat
   :any
   [:? [:vector spec.tg/MessageEntity]]])


(def SendTextBlueprintEntry
  [:cat
   [:fn is-ns-kw?]
   SendTextBlueprintEntryArgs])


(def ClickBtnBlueprintEntryArgs
  [:cat
   [:? [:maybe [:int {:min 1}]]]
   [:or :string Regexp]])


(def ClickBtnBlueprintEntry
  [:cat
   [:fn is-ns-kw?]
   ClickBtnBlueprintEntryArgs])


(def CheckMessageBlueprintEntryArgs
  [:cat
   [:? [:maybe [:int {:min 1}]]]
   [:? :string]
   [:? Regexp]
   [:? [:sequential spec.tg/MessageEntity]]
   [:? [:vector [:vector [:or :string Regexp]]]]])


(def CheckMessageBlueprintEntry
  [:cat
   [:fn is-ns-kw?]
   CheckMessageBlueprintEntryArgs])


(def BlueprintEntryArgs
  [:or
   SendTextBlueprintEntryArgs
   ClickBtnBlueprintEntryArgs
   CheckMessageBlueprintEntryArgs])


(def BlueprintEntry
  [:or
   SendTextBlueprintEntry
   ClickBtnBlueprintEntry
   CheckMessageBlueprintEntry])

(def Blueprint
  [:cat
   [:* BlueprintEntry]])
