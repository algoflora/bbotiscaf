(ns bbotiscaf.impl.handler
  (:require [taoensso.timbre :as log]
            [malli.core :as m]
            [bbotiscaf.misc :refer [validate!]]
            [bbotiscaf.spec.telegram :as spec.tg]
            [bbotiscaf.impl.system.app :as app]
            [bbotiscaf.impl.user :as u]
            [bbotiscaf.impl.api :as api]
            [bbotiscaf.impl.callback :as clb]
            [bbotiscaf.dynamic :refer [*dtlv* *user* *upd* *msg*]]
            [pod.huahaiy.datalevin :as d]))

(defmulti ^:private handle (fn [type _] type))

(def ^:private types #{:message :callback_query :pre_checkout_query})

(m/=> handle-update [:-> spec.tg/Update :nil])
(defn handle-update
  [update]
  (log/info ::handle-update "Handling Update:\t%s " update {:update update})
  (let [type (some types (keys update))]
    (if (= "private" (get-in update [type :chat :type]))
      (d/with-transaction [conn @app/db-conn]
        (binding [*dtlv* conn]
          (binding [*user* (u/load-or-create (get-in update [type :from]))
                    *upd*  update]
            (handle type (type update)))))
      (log/warn ::non-private-chat-update
                "Message from non-private chat!"
                {:update update}))))

(defn- reset
  []
  (u/set-msg-id *user* 0)
  (log/warn ::msg-id-reset-warning
            "Reset of msg-id called!"
            {:update *upd* :user *user*})
  (handle-update *upd*))


(defmethod handle :message
  [_ msg]
  (validate! spec.tg/Message msg)
  (log/info ::handle-message "Handling Message:\t%s " msg {:message msg})
  (if (and (= "/start" (:text msg)) (some? (:user/msg-id *user*)) (not= 0 (:user/msg-id *user*)))
    (reset)
    (-> *user* :user/uuid (clb/call msg)))
  (api/delete-message *user* (:message_id msg)))

(defmethod handle :callback_query
  [_ cbq]
  (binding [*msg* (-> cbq :message :message_id)]
    (-> cbq :data java.util.UUID/fromString clb/call)))
