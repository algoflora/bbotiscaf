(ns bbotiscaf.impl.e2e.dummy
  (:require [malli.core :as m]
            [bbotiscaf.impl.system.app :as app]
            [bbotiscaf.spec.telegram :as spec.tg]
            [bbotiscaf.spec.e2e :as spec.e2e]
            [clojure.pprint :refer [pprint]]))

(defonce ^:private dummies (atom {}))

(m/=> create [:function
              [:-> :keyword spec.tg/User-tg]
              [:=> [:cat :keyword :keyword] spec.tg/User-tg]])
(defn- create
  ([key] (create key @app/default-language-code))
  ([key lang]
   {:id (inc (count @dummies))
    :is_bot false
    :first_name (name key)
    :username (name key)
    :language_code (name lang)}))

(m/=> new [:-> :keyword [:map-of :keyword spec.e2e/Dummy-Entry]])
(defn new
  [key]
  (swap! dummies assoc :key {:dummy (create key) :messages []}))

(defn dbg [x] (println "DBG\t" x) x)

(m/=> get-by-chat-id [:-> :int spec.e2e/Dummy-Entry])
(defn- get-by-chat-id
  [chat-id]
  (->> (vals @dummies)
       (filter #(= (get-in % [:dummy :id]) chat-id))
       first))

(m/=> get-last-messages [:=> [:cat spec.tg/User-tg :int :boolean] [:vector spec.tg/Message]])
(defn get-last-messages
  [dummy n own?]
  (->> dummy :username keyword ((deref dummies)) :messages
       (filter #(= own? (= (:id dummy) (-> % :from :id)))) (take-last n) (into [])))


(m/=> add-message [:-> spec.tg/Send-Message-Request spec.tg/Message])
(defn add-message
  [msg]
  (let [{:keys [dummy messages]} (get-by-chat-id (:chat_id msg))
        key (-> dummy :username keyword)]
    (swap! dummies (fn [dms]
                     (update-in dms [key :messages]
                                conj (assoc (dissoc msg :chat_id)
                                            :message_id (-> messages
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
                                            :date (System/currentTimeMillis)))))
    (first (get-last-messages dummy 1 false))))

(m/=> delete-message [:=> [:cat :int :int] :boolean])
(defn delete-message
  [chat-id message-id]
  (let [dummy (:dummy (get-by-chat-id chat-id))
        key (-> dummy :username keyword)]
    (swap! dummies (fn [dms]
                     (update-in dms [key :messages]
                                (fn [msgs] (filter #(not= message-id (:message_id %)) msgs)))))
    ;; TODO: Maybe just `true` later
    (empty? (filter #(= message-id (:message_id %)) (-> @dummies key :messages)))))
