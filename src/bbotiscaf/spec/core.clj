(ns bbotiscaf.spec.core
  (:require [bbotiscaf.spec.telegram :as spec.tg]
            [bbotiscaf.spec.action :as spec.act]))

(def request-schema
  [:or
   spec.tg/update-schema
   spec.act/action-request-schema])

(def user-opts
  [:map
   {:closed true}
   [:lambda-name :string]
   [:cluster :string]
   [:lambda-memory-size {:optional true} [:int {:min 128 :max 10240}]]
   [:lambda-timeout {:optional true} [:int {:min 1 :max 900}]]
   [:lambda-env-vars {:optional true} [:vector {:min 0} :string]]
   [:datalevin-version {:optional true} :string]
   [:tfstate-bucket {:optional true} :string]])
