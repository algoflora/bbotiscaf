(ns bbotiscaf.impl.timer)

(defonce ^:private timer (atom (system-time)))


(defn reset-timer!
  []
  (reset! timer (system-time)))


(defn millis-passed
  []
  (* 0.000001 (- (system-time) @timer)))
