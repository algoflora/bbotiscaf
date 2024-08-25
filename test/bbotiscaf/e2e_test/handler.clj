(ns bbotiscaf.e2e-test.handler
  (:require [bbotiscaf.dynamic :refer [*user*]]
            [bbotiscaf.api :as api]
            [bbotiscaf.button :as b]))

(defn main
  [msg]
  (let [text (:text msg)]
    (api/send-message *user*
                      (format "Hi, %s!" (or text "stranger"))
                      [[(b/text-btn "Back" 'bbotiscaf.e2e-test.handler/main)]])))
