(ns bbotiscaf.impl.e2e
  (:require [bbotiscaf.core :as bbot]
            [bbotiscaf.spec.telegram :as spec.tg]
            [malli.core :as m]))

(defonce update-id (atom 0))

(m/=> send-update [:-> spec.tg/Update-Data :nil])
(defn send-update
  [data]
  (let [update (assoc data :update_id (swap! update-id inc))]
    (#'bbot/handler update)))

(defn request
  [& args]
  (println args))
