(ns bbotiscaf.impl.e2e.client
  (:require
    [bbotiscaf.core :as bbot]
    [bbotiscaf.impl.e2e.dummy :as dum]
    [bbotiscaf.misc :refer [throw-error]]
    [bbotiscaf.spec.commons :refer [Regexp]]
    [bbotiscaf.spec.telegram :as spec.tg]
    [malli.core :as m]))


(defonce ^:private update-id (atom 0))

(m/=> send-update [:-> spec.tg/Update-Data :any])


(defn- send-update
  [data]
  (let [update (assoc data :update_id (swap! update-id inc))]
    (#'bbot/handler update)))


(m/=> dummy-kw?->dummy [:-> [:or spec.tg/User :keyword] spec.tg/User])


(defn- dummy-kw?->dummy
  [dummy]
  (if (keyword? dummy) (-> dummy dum/get-by-key :dummy) dummy))


(m/=> dummy->base-message [:-> [:or spec.tg/User :keyword] spec.tg/BaseMessage])


(defn- dummy->base-message
  [dummy-kw?]
  (let [dummy (dummy-kw?->dummy dummy-kw?)
        message-id (-> (dum/get-last-messages dummy 1 nil) first :message_id (or 0) inc)]
    {:message_id message-id
     :from dummy
     :chat {:id (:id dummy)
            :type "private"}
     :date (System/currentTimeMillis)}))


(m/=> send-text [:=>
                 [:cat [:or spec.tg/User :keyword] :string [:vector spec.tg/MessageEntity]]
                 :any])


(defn send-text
  [dummy text entities]
  (let [message (merge (dummy->base-message dummy)
                       {:text text :entities entities})]
    (dum/add-message message)
    (send-update {:message message})))


(m/=> dummy->base-callback-query [:-> [:or spec.tg/User :keyword] spec.tg/BaseCallbackQuery])


(defn- dummy->base-callback-query
  [dummy-kw?]
  (let [dummy (dummy-kw?->dummy dummy-kw?)]
    {:id (str (java.util.UUID/randomUUID))
     :from dummy}))


(m/=> click-btn [:=>
                 [:cat
                  [:or spec.tg/User :keyword]
                  spec.tg/Message
                  Regexp]
                 :any])


(defn click-btn
  [dummy msg btn-re]
  (let [base-cbq (merge (dummy->base-callback-query dummy))
        buttons  (->> msg :reply_markup :inline_keyboard flatten
                      (filter #(some? (re-find btn-re (:text %)))))]
    (cond
      (< 1 (count buttons))
      (throw-error ::ambiguous-buttons-found "Ambiguous buttons found!"
                   {:message msg :regex btn-re  :buttons buttons :dummy dummy})

      (zero? (count buttons))
      (throw-error ::button-not-found "Button not found!"
                   {:message msg :regex btn-re :dummy dummy})

      :else
      (send-update {:callback_query
                    (merge base-cbq
                           {:message msg :data (-> buttons first :callback_data)})}))))
