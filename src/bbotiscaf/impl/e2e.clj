(ns bbotiscaf.impl.e2e
  (:require [bbotiscaf.spec.telegram :as spec.tg]
            [bbotiscaf.impl.e2e.dummy :as dum]
            [malli.core :as m]
            [clojure.pprint :refer [pprint]]
            [taoensso.timbre :as log]))

(defmulti ^:private serve (fn [method _] method))

(m/=> request [:=> [:cat :keyword spec.tg/Request] :any])
(defn request
  [method body]
  (log/info ::request-received
            "Received %s request" method
            {:method method
             :body body})
  (serve method body))

(defmethod serve :sendMessage
  [_ msg]
  (dum/add-message msg))

(defmethod serve :deleteMessage
  [_ {:keys [chat_id message_id]}]
  (dum/delete-message chat_id message_id))
