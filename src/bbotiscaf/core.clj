(ns bbotiscaf.core
  (:gen-class)
  (:require
    [bbotiscaf.dynamic :refer [*dtlv*]]
    [bbotiscaf.impl.api :as api]
    [bbotiscaf.impl.handler :as h]
    [bbotiscaf.impl.system :as sys]
    [bbotiscaf.impl.system.app :as app]
    [bbotiscaf.impl.timer :refer [reset-timer!]]
    [bbotiscaf.logging :as logging]
    [bbotiscaf.spec.action :as spec.act]
    [bbotiscaf.spec.aws :as spec.aws]
    [bbotiscaf.spec.core :as spec]
    [bbotiscaf.spec.telegram :as spec.tg]
    [cheshire.core :as json]
    [datalevin.core :as d]
    [malli.core :as m]
    [taoensso.timbre :as log]))


(m/=> handle-action [:-> spec.act/ActionRequest :any])


(defn- handle-action
  [{:keys [action] {:keys [method arguments]} :action}]
  (if-let [action-fn (resolve (symbol (app/handler-actions-namespace) method))]
    (log/info ::action-success
              "Action '%s' completed successfully" type
              {:action action
               :ok true
               :response (action-fn arguments)})
    (throw (ex-info "Wrong action type!"
                    {:event ::wrong-action-error
                     :action-type type}))))


(m/=> handler [:-> spec/Request :any])


(defn handler
  [req]
  (log/debug ::core-handler {:request req})
  (d/with-transaction [conn (app/db-conn)]
                      (binding [*dtlv* conn]
                        (cond
                          (m/validate spec.tg/Update req)
                          (h/handle-update req)

                          (m/validate spec.act/ActionRequest req)
                          (handle-action req)))
                      (api/flush-api)))


(m/=> log-and-prepare [:-> spec.aws/SQSRecordSchema spec/Request])


(defn- log-and-prepare
  [rec]
  (reset-timer!)
  (log/debug ::handling-record
             "Handling SQS record. %s" rec
             {:record rec})
  (-> rec :body (json/parse-string true)))


(defn- setup-logs!
  [context]
  (logging/set-lambda-context! context)
  (logging/inject-lambda-context!))


(m/=> sqs-receiver [:=> [:cat
                         spec.aws/SQSRecordsBunch
                         spec.aws/SQSContext] :any])


(defn sqs-receiver
  [records context]
  (setup-logs! context)
  (sys/startup!)
  (try
    (let [rs (:Records records)]
      (log/info ::sqs-message-received
                "Received SQS message. %d records." (count rs)
                {:records-count (count rs)
                 :records rs
                 :context context})
      (doseq [r rs]
        (-> r log-and-prepare handler)))
    (finally
      (sys/shutdown!))))


(defn -main
  [& args]
  (apply sqs-receiver args))
