(ns bbotiscaf.spec.blueprint
  (:require
    [bbotiscaf.spec.commons :refer [Regexp]]
    [bbotiscaf.spec.telegram :as spec.tg]))


(def SendTextBlueprintEntryArgs
  [:cat
   :any
   [:? [:vector spec.tg/MessageEntity]]])


(def SendTextBlueprintEntry
  [:cat
   [:fn qualified-keyword?]
   SendTextBlueprintEntryArgs])


(def ClickBtnBlueprintEntryArgs
  [:cat
   [:? [:maybe [:int {:min 1}]]]
   [:or :string Regexp]])


(def ClickBtnBlueprintEntry
  [:cat
   [:fn qualified-keyword?]
   ClickBtnBlueprintEntryArgs])


(def CheckMessageBlueprintEntryArgs
  [:cat
   [:? [:maybe [:int {:min 1}]]]
   [:? [:or :string Regexp]]
   [:? [:set spec.tg/MessageEntity]]
   [:? [:vector [:vector [:or :string Regexp]]]]])


(def CheckMessageBlueprintEntry
  [:cat
   [:fn qualified-keyword?]
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
