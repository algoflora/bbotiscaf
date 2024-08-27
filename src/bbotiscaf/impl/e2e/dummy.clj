(ns bbotiscaf.impl.e2e.dummy
  (:require
    [bbotiscaf.impl.system.app :as app]
    [bbotiscaf.misc :refer [throw-error remove-nils]]
    [bbotiscaf.spec.e2e :as spec.e2e]
    [bbotiscaf.spec.telegram :as spec.tg]
    [malli.core :as m]))


(defonce ^:private dummies (atom {}))


(m/=> create [:function
              [:-> :keyword spec.tg/User]
              [:=> [:cat :keyword :keyword] spec.tg/User]])


(defn- create
  ([key] (create key @app/default-language-code))
  ([key lang]
   {:id (inc (count @dummies))
    :is_bot false
    :first_name (name key)
    :username (name key)
    :language_code (name lang)}))


(m/=> new [:-> :keyword spec.e2e/Dummy-Entry])


(defn new
  [key]
  (key (swap! dummies assoc key {:dummy (create key) :messages []})))


(defn clear-all
  []
  (reset! dummies {}))


(m/=> dump-all [:=> [:cat] [:map-of :keyword spec.e2e/Dummy-Entry]])


(defn dump-all
  []
  @dummies)


(m/=> restore [:-> [:maybe [:map-of :keyword spec.e2e/Dummy-Entry]] :any])


(defn restore
  [data]
  (reset! dummies (or data {})))


(defn dbg
  [x]
  (println "DBG\t" x) x)


(m/=> get-by-chat-id [:-> :int spec.e2e/Dummy-Entry])


(defn- get-by-chat-id
  [chat-id]
  (->> (vals @dummies)
       (filter #(= (get-in % [:dummy :id]) chat-id))
       first))


(m/=> exists? [:-> :keyword :boolean])


(defn exists?
  [key]
  (contains? @dummies key))


(m/=> get-by-key [:-> :keyword spec.e2e/Dummy-Entry])


(defn get-by-key
  [key]
  (key @dummies))


(m/=> get-last-messages [:=>
                         [:cat spec.tg/User :int [:or :boolean :nil]]
                         [:vector spec.tg/Message]])


(defn get-last-messages
  [dummy n own?]
  (->> dummy :username keyword ((deref dummies)) :messages
       (filter #(case own?
                  true  (= (:id dummy) (-> % :from :id))
                  false (not= (:id dummy) (-> % :from :id))
                  nil   true))
       (take-last n)
       (into [])))


(m/=> get-first-message [:-> spec.tg/User spec.tg/Message])


(defn get-first-message
  [dummy]
  (-> dummy :username keyword ((deref dummies)) :messages first))


(m/=> add-message [:=> [:cat [:or spec.tg/SendMessageRequest spec.tg/Message]] spec.tg/Message])


(defn add-message
  [req]
  (let [{:keys [dummy messages]} (get-by-chat-id (or (:chat_id req) (-> req :chat :id)))]
    (swap! dummies (fn [dms]
                     (update-in dms [(-> dummy :username keyword) :messages]
                                conj (merge {:message_id (-> messages
                                                             first
                                                             :message_id
                                                             (or 0)
                                                             inc)
                                             :from {:id 0
                                                    :is_bot true
                                                    :first_name "bbotiscaf"
                                                    :last_name "e2e"
                                                    :username "bbotiscaf.e2e"
                                                    :language_code "clj"}
                                             :chat {:id (:id dummy)
                                                    :type "private"}
                                             :date (System/currentTimeMillis)}
                                            (dissoc req :chat_id)))))
    (first (get-last-messages dummy 1 nil))))


(m/=> find-message-index [:=> [:cat [:vector spec.tg/Message] :int] :int])


(defn- find-message-index
  [messages message-id]
  (let [idxs (keep-indexed #(when (= message-id (:message_id %2)) %1) messages)]
    (cond
      (< 1 (count idxs))   (throw-error ::ambiguous-messages-found "Ambiguous messages found!"
                                        {:messages messages :message_id message-id})
      (zero? (count idxs)) (throw-error ::message-not-found "Message not found!"
                                        {:messages messages :message_id message-id})
      :else (first idxs))))


(m/=> update-message-text [:-> spec.tg/EditMessageTextRequest spec.tg/Message])


(defn update-message-text
  [req]
  (let [{:keys [dummy messages]} (get-by-chat-id (or (:chat_id req) (-> req :chat :id)))
        msg-idx (find-message-index messages (:message_id req))
        key (-> dummy :username keyword)
        dummies# (swap! dummies
                        (fn [dms]
                          (update-in
                           dms [key :messages]
                           (fn [msgs]
                             (update msgs
                                     msg-idx
                                     #(merge % (remove-nils
                                                {:text (:text req)
                                                 :entities (:entities req)
                                                 :reply_markup (:reply_markup req)})))))))]
    (get-in dummies# [key :messages msg-idx])))


(m/=> delete-message [:=> [:cat :int :int] :boolean])


(defn delete-message
  [chat-id message-id]
  (let [dummy (:dummy (get-by-chat-id chat-id))
        key (-> dummy :username keyword)]
    (swap! dummies (fn [dms]
                     (update-in dms [key :messages]
                                (fn [msgs]
                                  (into [] (filter #(not= message-id (:message_id %)) msgs))))))
    ;; TODO: Maybe just `true` later
    (empty? (filter #(= message-id (:message_id %)) (-> @dummies key :messages)))))
