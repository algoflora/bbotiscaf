(ns bbotiscaf.spec.blueprint
  (:require
    [bbotiscaf.spec.commons :refer [Regexp]]
    [bbotiscaf.spec.telegram :as spec.tg]))


(def SendTextBlueprintEntry
  [:cat
   :string
   [:? [:vector spec.tg/MessageEntity]]])


(def ClickBtnBlueprintEntry
  [:cat
   [:? [:maybe [:int {:min 1}]]]
   [:or :string Regexp]])


(def CheckMessageBlueprintEntry
  [:cat
   [:? [:maybe [:int {:min 1}]]]
   [:? :string]
   [:? Regexp]
   [:? [:sequential spec.tg/MessageEntity]]
   [:? [:vector [:vector [:or :string Regexp]]]]])


(defn is-ns-kw?
  [x]
  (and (keyword? x) (some? (namespace x))))


(def BlueprintEntry
  [:cat
   [:fn is-ns-kw?]
   [:or
    SendTextBlueprintEntry
    ClickBtnBlueprintEntry
    CheckMessageBlueprintEntry]])

(def Blueprint
  [:* BlueprintEntry])
