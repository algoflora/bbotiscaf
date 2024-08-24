(ns bbotiscaf.spec.e2e
  (:require [bbotiscaf.spec.telegram :as spec.tg]))

(def Dummy-Entry
  [:map
   [:dummy spec.tg/User-tg]
   [:messages [:vector spec.tg/Message]]])
