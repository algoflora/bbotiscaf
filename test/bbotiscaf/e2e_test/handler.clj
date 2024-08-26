(ns bbotiscaf.e2e-test.handler
  (:require [bbotiscaf.dynamic :refer [*user*]]
            [bbotiscaf.api :as api]
            [bbotiscaf.button :as b]))

(defn main
  [msg]
  (let [text (or (:text msg) "stranger")]
    (api/send-message *user*
                      (format "Hi, %s!" text)
                      [[(b/text-btn "Go!" 'bbotiscaf.e2e-test.handler/go {:text text})]])))

(defn go
  [{:keys [text]}]
  (api/send-message *user* (format "Go, %s!" text) [[(b/home-btn "Home")]]))
