(ns bbotiscaf.spec.core
  (:require [bbotiscaf.spec.telegram :as spec.tg]
            [bbotiscaf.spec.action :as spec.act]))

(def request-schema
  [:or
   spec.tg/update-schema
   spec.act/action-request-schema])
