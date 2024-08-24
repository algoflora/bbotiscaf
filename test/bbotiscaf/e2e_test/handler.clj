(ns bbotiscaf.e2e-test.handler
  (:require [bbotiscaf.dynamic :refer [*user*]]
            [bbotiscaf.api :as api]
            [bbotiscaf.button :as b]))

(defn main
  [msg]
  (let [text (:text msg)]
    (println "Hello everybody!")
    (api/send-message *user* text [[(b/text-btn "Hi!" 'bbotiscaf.e2e-test.handler/main)]])))
