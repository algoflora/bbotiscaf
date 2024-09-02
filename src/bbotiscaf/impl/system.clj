(ns bbotiscaf.impl.system
  (:require
    ;; [bbotiscaf.impl.api]
    [bbotiscaf.impl.config :as conf]
    [bbotiscaf.impl.e2e]
    [bbotiscaf.impl.system.app :as app]
    [bbotiscaf.misc :refer [read-resource-dir throw-error]]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [integrant.core :as ig]
    [malli.core :as m]
    [malli.instrument :as mi]
    [taoensso.timbre :as log]))


(require '[pod.huahaiy.datalevin :as d])


(defn- get-bbotiscaf-schema
  []
  (-> "bbotiscaf-resources/schema.edn"
      io/resource
      slurp
      edn/read-string))


(defmethod ig/init-key :api/fn
  [_ symbol]
  (log/debug ::init-api-fn
             "Applying :api/fn %s..." symbol
             {:symbol symbol})
  (find-var symbol))


(defmethod ig/init-key :db/conn
  [_ conn-str]
  (let [schema (merge (get-bbotiscaf-schema) (read-resource-dir "schema"))
        opts   {:validate-data? true
                :closed-schema? true
                :auto-entity-time? true}
        cn-str (or conn-str (format "/tmp/bbotiscaf-test/%s" (str (java.util.UUID/randomUUID))) #_(str (fs/create-temp-dir {:prefix "bbotiscaf-test"})))]
    (log/debug ::init-db-conn
               "Applying :db/conn %s..." cn-str
               {:conn-str cn-str
                :schema schema
                :opts opts})
    (d/get-conn cn-str schema #_opts)))


(defmethod ig/halt-key! :db/conn
  [_ conn]
  (log/debug ::halt-db-conn
             "Halting :db/conn...")
  (d/close conn))


(defmethod ig/init-key :bot/token
  [_ token]
  (log/debug ::init-bot-token
             "Applying :bot/token %s..." token
             {:token token})
  token)


(defmethod ig/init-key :bot/default-language-code
  [_ code]
  (log/debug ::init-bot-default-language-code
             "Applying :bot/default-language-code %s..." code
             {:code code})
  code)


(defmethod ig/init-key :handler/namespaces
  [_ namespaces]
  (log/debug ::init-handler-namespaces
             "Applying :handler/namespaces %s..." namespaces
             {:namespaces namespaces})
  namespaces)


(defmethod ig/init-key :handler/main
  [_ handler]
  (log/debug ::init-handler-main
             "Applying :handler/main %s..." handler
             {:handler handler})
  handler)


(defn- malli-instrument-error-handler
  [error data]
  (let [func (:fn-name data)
        expl (m/explain (:schema data) (:value data))]
    (throw-error ::malli-instrument-error
                 (format "Malli instrument error in function '%s'!" func)
                 {:funtion func :explanation expl :error error :data data})))


(defn startup!
  []
  (when-not (app/app-set?)
    (mi/instrument! {:report malli-instrument-error-handler})
    (let [config (conf/get-config)]
      (app/set-app! (ig/init config))
      (log/info ::startup-completed
                "Startup completed: %s" (app/get-app)))))


(defn shutdown!
  []
  (ig/halt! (app/get-app))
  (app/clear-app!)
  (log/info ::sutdown-completed
            "Shutdown completed!"))
