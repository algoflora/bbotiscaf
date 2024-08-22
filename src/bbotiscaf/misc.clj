(ns bbotiscaf.misc
  (:require [malli.core :as m]
            [taoensso.timbre :as log]))

(defn ex->map
  [ex]
  {:message    (ex-message ex)
   :data       (ex-data ex)
   :cause      (ex-cause ex)
   :stacktrace (.getStackTrace ex)})

(defn validate!
  [schema value]
  (when-not (m/validate schema value)
    (let [data {:explanation (m/explain schema value)}]
      (log/error ::validation-failed
                 "Validation failed!"
                 data)
      (throw (ex-info "Validation failed!" data)))))
