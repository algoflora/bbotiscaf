(ns bbotiscaf.core
  (:require [actions]
            [bbotiscaf.logging :as logging]
            [taoensso.timbre :as log]
            [cheshire.core :as json]
            [malli.core :as m]
            [malli.instrument :as mi]
            [bbotiscaf.misc :refer [ex->map]]
            [bbotiscaf.spec.core :as spec]
            [bbotiscaf.spec.aws :as spec.aws]
            [bbotiscaf.spec.telegram :as spec.tg]
            [bbotiscaf.spec.action :as spec.act]
            [babashka.pods :as pods]
            [babashka.process :refer [shell]]
            [clojure.pprint :refer [pprint]]))

(time (pods/load-pod "dtlv"))
(time (require '[pod.huahaiy.datalevin :as d]))

(defn do-things
  []
  (let [conn (time (d/get-conn "/mnt/dtlv/bbotiscaf-test-1"))]

    (try
      (d/with-transaction [cn conn]
        (time (d/transact! cn [{:user/uuid (random-uuid)
                                :user/name "Ivan"}]))
        (time (d/q '[:find (pull ?u [*]) :where [?u :user/name]] (d/db cn))))
      #_(finally (d/close conn))))
  nil)

(m/=> handle-update [:=> [:cat spec.tg/update-schema] :nil])
(defn- handle-update
  [update]
  (log/info "Update:\t%s " update {:request update}))

(m/=> handle-action [:=> [:cat spec.act/action-request-schema] :nil])
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
      (log/error ::action-failure
                 (.getMessage ex)
                 {:action action
                  :ok false
                  :error (ex->map ex)}))))

(m/=> handler [:=> [:cat spec/request-schema] :nil])
(defn- handler
  [req]
  (cond
    (m/validate spec.tg/update-schema req)
    (handle-update req)
  
    (m/validate spec.act/action-request-schema req)
    (handle-action req)))

(defn setup-logs!
  [context]
  (logging/set-lambda-context! context)
  (logging/inject-lambda-context!))

(m/=> sqs-receiver [:=> [:cat
                         spec.aws/sqs-records-bunch-schema
                         spec.aws/sqs-context-schema] :nil])
(defn sqs-receiver
  [records context]
  (setup-logs! context)
  (let [rs (:Records records)]
    (log/info "Received SQS message. %d records." (count rs)
              {:records-count (count rs)
               :records rs
               :context context})
    (doseq [r rs]
      (handler (-> r :body (json/parse-string true))))
    (do-things)))

(defn malli-instrument-error-handler [error data]
  (log/error ::malli-instrument-error
             "Malli instrumentation error in function '%s'!" (:fn-name data)
             {:error error
              :data data})
  (throw (ex-info "Malli instrumentation error" {:error error :data data})))

;(mi/instrument! {:report malli-instrument-error-handler})
