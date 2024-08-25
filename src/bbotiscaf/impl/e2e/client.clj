(ns bbotiscaf.impl.e2e.client
  (:require [malli.core :as m]
            [bbotiscaf.core :as bbot]
            [bbotiscaf.spec.telegram :as spec.tg]
            [bbotiscaf.impl.e2e.dummy :as dum]
            [clojure.walk :refer [postwalk]]))

(defonce ^:private update-id (atom 0))

(m/=> send-update [:-> spec.tg/Update-Data :any])
(defn- send-update
  [data]
  (let [update (assoc data :update_id (swap! update-id inc))]
    (#'bbot/handler update)))

(m/=> denil [:-> :map :map])
(defn- denil
  [in-map]
  (postwalk (fn [item] (if (map? item) (into {} (filter #(-> % val some?) item)) item)) in-map))

(m/=> dummy->base-message [:-> [:or spec.tg/User :keyword] spec.tg/Base-Message])
(defn- dummy->base-message
  [dummy]
  (let [dummy      (if (keyword? dummy) (-> dummy dum/get-by-key :dummy) dummy)
        message-id (-> (dum/get-last-messages dummy 1 nil) first :message_id (or 0) inc)]
    {:message_id message-id
     :from dummy
     :chat {:id (:id dummy)
            :type "private"}
     :date (System/currentTimeMillis)}))

(m/=> send-text [:=>
                 [:cat [:or spec.tg/User :keyword] :string [:vector spec.tg/Message-Entity]]
                 :any])
(defn send-text
  [dummy text entities]
  (let [message (merge (dummy->base-message dummy)
                       {:text text :entities entities})]
    (dum/add-message message)
    (send-update {:message message})))
