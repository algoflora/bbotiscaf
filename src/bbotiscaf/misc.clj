(ns bbotiscaf.misc)

(defn ex->map
  [ex]
  {:message    (ex-message ex)
   :data       (ex-data ex)
   :cause      (ex-cause ex)
   :stacktrace (.getStackTrace ex)})
