(ns bbotiscaf.core
  (:require [actions]
            [bbotiscaf.impl.handler :as h]
            [bbotiscaf.logging :as logging]
            [taoensso.timbre :as log]
            [cheshire.core :as json]
            [malli.core :as m]
            [bbotiscaf.misc :refer [throw-error]]
            [bbotiscaf.spec.core :as spec]
            [bbotiscaf.spec.aws :as spec.aws]
            [bbotiscaf.spec.telegram :as spec.tg]
            [bbotiscaf.spec.action :as spec.act]))

(m/=> handle-action [:-> spec.act/Action-Request :nil])
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
  
    (m/validate spec.act/Action-Request req)
    (handle-action req)))

(defn- setup-logs!
  [context]
  (logging/set-lambda-context! context)
  (logging/inject-lambda-context!))

(m/=> sqs-receiver [:=> [:cat
                         spec.aws/SQS-Records-Bunch
                         spec.aws/SQS-Context] :nil])
(defn sqs-receiver
  [records context]
  (setup-logs! context)
  (let [rs (:Records records)]
    (log/info "Received SQS message. %d records." (count rs)
              {:records-count (count rs)
               :records rs
               :context context})
    (doseq [r rs]
      (handler (-> r :body (json/parse-string true))))))
