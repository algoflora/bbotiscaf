(ns bbotiscaf.spec.core
  (:require [bbotiscaf.spec.telegram :as spec.tg]
            [bbotiscaf.spec.event :as spec.ev]))

(def request-schema
  [:or
   spec.tg/update-schema
   spec.ev/event-schema])
