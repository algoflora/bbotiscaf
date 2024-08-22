(ns bbotiscaf.impl.system
  (:require [integrant.core :as ig]
            [babashka.fs :as fs]
            [bbotiscaf.misc :refer [ex->map]]
            [bbotiscaf.impl.config :as conf]
            ;; [bbotiscaf.impl.api]
            [bbotiscaf.impl.e2e]
            [malli.instrument :as mi]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [pod.huahaiy.datalevin :as d]))

(def ^:private app (atom nil))

(defn get-db-conn [] (:db/conn @app))

(defn get-main-handler [] (:handler/main @app))\

(defn get-handler-namespaces [] (:handler/namespaces @app))

(defn- malli-instrument-error-handler [error data]
  (log/error ::malli-instrument-error
             "Malli instrumentation error in function '%s'!" (:fn-name data)
             {:error error
              :data data})
  (throw (ex-info "Malli instrumentation error" {:error error :data data})))

(defn startup!
  []
  (mi/instrument! {:report malli-instrument-error-handler})
  (let [config (conf/get-config)]
    (reset! app (ig/init config))
    (log/info ::startup-completed
              "Startup completed: %s" @app
              {:app @app})))

(defn shutdown!
  []
  (ig/halt! @app))


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
  token)

(defmethod ig/init-key :handler/namespaces
  [_ namespaces]
  namespaces)

(defmethod ig/init-key :handler/main
  [_ main]
  main)
