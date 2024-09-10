(ns bbotiscaf.e2e-test.handler
  (:require
    [bbotiscaf.api :as api]
    [bbotiscaf.button :as b]
    [bbotiscaf.dynamic :refer [*user* *dtlv* dtlv]]
    [bbotiscaf.user :as u]
    [clojure.string :as str]))


(require '[pod.huahaiy.datalevin :as d])


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


(defn store
  [_]
  (api/send-message *user* "Hello" [[(b/text-btn "Save" 'bbotiscaf.e2e-test.handler/save)]]))


(defn save
  [_]
  (d/transact! *dtlv* [{:test-entity/user [:user/id (:user/id *user*)]
                        :test-entity/data (str/upper-case (:user/first-name *user*))}])
  (api/send-message *user* "Name saved" [[(b/text-btn "Reveal" 'bbotiscaf.e2e-test.handler/reveal)]]))


(defn reveal
  [_]
  (let [name (ffirst (d/q '[:find ?n
                            :in $ ?uid
                            :where
                            [?u :user/id ?uid]
                            [?e :test-entity/user ?u]
                            [?e :test-entity/data ?n]] (dtlv) (:user/id *user*)))]
    (api/send-message *user* name [])))


(defn roled
  [_]
  (let [text (if (u/has-role? :admin) "Hello, sir" "Hi")]
    (api/send-message *user* text [])))
