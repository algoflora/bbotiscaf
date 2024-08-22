(ns bbotiscaf.impl.system
  (:require [integrant.core :as ig]
            [babashka.fs :as fs]
            [bbotiscaf.misc :refer [ex->map]]
            [bbotiscaf.impl.config :as conf]
            ;; [bbotiscaf.impl.api]
            [bbotiscaf.impl.e2e]
            [bbotiscaf.impl.system.app :as app]
            [malli.core :as m]
            [malli.instrument :as mi]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [pod.huahaiy.datalevin :as d]))


(defn- malli-instrument-error-handler [error data]
  (let [func (:fn-name data)
        [info err-str] (cond
                         (= :malli.core/invalid-input error)
                         [(m/explain (:input data) (:args data)) "invalid input"]

               :else
               [{:error error :data data} "instrumentation"])]
    (log/error ::malli-instrument-error
               "Malli %s error in function '%s'! %s" err-str func info)
    (throw (ex-info "Malli instrumentation error" {:error error :info info :function func}))))

(defn startup!
  []
  (when-not (app/app-set?)
    (mi/instrument! {:report malli-instrument-error-handler})
    (let [config (conf/get-config)]
      (app/set-app! (ig/init config))
      (log/info ::startup-completed
                "Startup completed: %s" (app/get-app)))))

(defn- get-bbotiscaf-schema
  []
  (-> "bbotiscaf-resources/schema.edn"
      io/resource
      slurp
      edn/read-string))

(defn- get-project-schema
  []
  (some->> (some-> "schema"
                   io/resource
                   (fs/glob "**.edn")
                   flatten)
           (map #(-> % slurp edn/read-string))
           (apply merge)))

(defmethod ig/init-key :api/fn
  [_ symbol]
  (log/info ::apply-api-fn
            "Applying :api/fn %s..." symbol
            {:symbol symbol})
  (find-var symbol))

(defmethod ig/init-key :db/conn
  [_ conn-str]
  (let [schema (merge (get-bbotiscaf-schema) (get-project-schema))
        opts {:validate-data? true
              :closed-schema? true
              :auto-entity-time? true}]
    (log/info ::apply-db-conn
              "Applying :db/conn %s..." conn-str
              {:conn-str conn-str
               :schema schema
               :opts opts})
    (d/get-conn conn-str schema #_opts)))

(defmethod ig/init-key :bot/token
  [_ token]
  (log/info ::apply-bot-token
            "Applying :bot/token %s..." token
            {:token token})
  token)

(defmethod ig/init-key :handler/namespaces
  [_ namespaces]
  (log/info ::apply-handler-namespaces
            "Applying :handler/namespaces %s..." namespaces
            {:namespaces namespaces})
  namespaces)

(defmethod ig/init-key :handler/main
  [_ handler]
  (log/info ::apply-handler-main
            "Applying :handler/main %s..." handler
            {:handler handler})
  handler)
