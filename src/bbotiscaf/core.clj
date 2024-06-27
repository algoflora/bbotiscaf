(ns bbotiscaf.core
  (:require [malli.core :as m]
            [malli.instrument :as mi]))

(defn handle-event
  [{:keys [event] {:keys [type args]} :event}]
  (if-let [func (resolve (ns events) (symbol type))]
    (func args)
    (throw (ex-info "Wrong event!" {:event event}))))

(defn handler
  [req]
  (cond
    true ;(m/validate req spec.ev/event-schema)
    nil)
  (println "REQ:" req (type req)))

(mi/instrument!)
 
;(handler 1)
