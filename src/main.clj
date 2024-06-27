(ns main
  (:require [cheshire.core :refer [parse-string]]
            [bbotiscaf.core :as bbot]))

(defn handler [& args]
  (println "Hello from BBotiscaf!")
  (bbot/handler (-> args first (parse-string true))))
