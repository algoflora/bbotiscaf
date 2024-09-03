(ns bbotiscaf.core
  (:require
    [actions]
    [bbotiscaf.impl.handler :as h]
    [bbotiscaf.impl.system :as sys]
    [bbotiscaf.logging :as logging]
    [bbotiscaf.misc :refer [throw-error]]
    [bbotiscaf.spec.action :as spec.act]
    [bbotiscaf.spec.aws :as spec.aws]
    [bbotiscaf.spec.core :as spec]
    [bbotiscaf.spec.telegram :as spec.tg]
    [cheshire.core :as json]
    [malli.core :as m]
    [taoensso.timbre :as log]))


(m/=> handle-action [:-> spec.act/ActionRequest :nil])


(defn- handle-action
  [{:keys [action] {:keys [type arguments]} :action}]
  (try
    (if-let [action-fn (resolve (symbol "actions" type))]
      (log/info ::action-success
                "Action '%s' completed successfully" type
                {:action action
                 :ok true
                 :response (action-fn arguments)})
      (throw (ex-info "Wrong action type!" {:action-type type})))
    (catch Exception ex
      (throw-error ::wrong-action-type (ex-message ex) (ex-data ex)))))


(m/=> handler [:-> spec/Request :nil])


(defn- handler
  [req]
  (cond
    (m/validate spec.tg/Update req)
    (h/handle-update req)

    (m/validate spec.act/ActionRequest req)
    (handle-action req)))


(m/=> log-and-handle [:-> spec.aws/SQSRecordSchema nil])


(defn- log-and-handle
  [rec]
  (log/debug ::handling-record
             "Handling SQS record. %s" rec
             {:record rec})
  (-> rec :body (json/parse-string true) handler))


(defn- setup-logs!
  [context]
  (logging/set-lambda-context! context)
  (logging/inject-lambda-context!))


(m/=> sqs-receiver [:=> [:cat
                         spec.aws/SQSRecordsBunch
                         spec.aws/SQSContext] :nil])


(defn sqs-receiver
  [records context]
  (setup-logs! context)
  (sys/startup!)
  (let [rs (:Records records)]
    (log/info ::sqs-message-received
              "Received SQS message. %d records." (count rs)
              {:records-count (count rs)
               :records rs
               :context context})
    (doseq [r rs]
      (log-and-handle r))))
