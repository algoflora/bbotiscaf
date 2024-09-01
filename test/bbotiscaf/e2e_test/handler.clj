(ns bbotiscaf.e2e-test.handler
  (:require
    [bbotiscaf.api :as api]
    [bbotiscaf.button :as b]
    [bbotiscaf.dynamic :refer [*user*]]
    [clojure.string :as str]))


(defn main
  [msg]
  (let [text (or (:text msg) "stranger")]
    (api/send-message *user*
                      (format "Hi, %s!" text)
                      [[(b/text-btn "Go!" 'bbotiscaf.e2e-test.handler/go {:text text})]
                       [(b/text-btn "Temp" 'bbotiscaf.e2e-test.handler/temp)]])))


(defn go
  [{:keys [text]}]
  (api/send-message *user* (format "Go, %s!" text) [[(b/home-btn "Home")]]))


(defn temp
  [_]
  (api/send-message *user*
                    (format "Temp message of %s" (-> *user* :user/first-name str/capitalize))
                    [] :temp))
