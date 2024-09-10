(ns bbotiscaf.impl.errors
  (:require
    [taoensso.timbre :as log]))


(defn handle-error
  ([ex] (handle-error (Thread/currentThread) ex))
  ([thr ex]
   (let [data (ex-data ex)
         ekw  (or (:event data) :error-event)
         msg  (ex-message ex)
         st   (take 5 (.getStackTrace ex))
         thrn (.getName thr)]
     (log/error ekw msg (merge data {:stacktrace st} {:thread thrn} {:is-error? true})))))
