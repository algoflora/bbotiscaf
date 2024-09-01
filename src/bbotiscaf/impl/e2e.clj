(ns bbotiscaf.impl.e2e
  (:require
    [bbotiscaf.impl.e2e.dummy :as dum]
    [bbotiscaf.spec.telegram :as spec.tg]
    [malli.core :as m]
    [taoensso.timbre :as log]))


(defmulti ^:private serve (fn [method _] method))

(m/=> request [:=> [:cat :keyword spec.tg/Request] :any])


(defn request
  [method body]
  (log/debug ::request-received
             "Received %s request" method
             {:method method
              :body body})
  (serve method body))


(defmethod serve :sendMessage
  [_ req]
  (dum/add-message req))


(defmethod serve :editMessageText
  [_ req]
  (dum/update-message-text req))


(defmethod serve :deleteMessage
  [_ {:keys [chat_id message_id]}]
  (dum/delete-message chat_id message_id))
