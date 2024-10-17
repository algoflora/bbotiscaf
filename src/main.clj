(ns main
  (:require
    [bbotiscaf.core :as bbot]))


(defn handler
  [& args]
  (println "Hello from BBotiscaf!")
  (apply bbot/sqs-receiver args))
