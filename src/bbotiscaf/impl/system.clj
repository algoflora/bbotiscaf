(ns bbotiscaf.impl.system
  (:require [integrant.core :as ig]
            [babashka.fs :as fs]
            [babashka.classpath :as cp]
            [bbotiscaf.misc :refer [ex->map]]
            ;; [bbotiscaf.impl.api]
            [bbotiscaf.impl.e2e]
            [malli.core :as m]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [aero.core :refer [read-config]]
            [taoensso.timbre :as log]
            [pod.huahaiy.datalevin :as d]))

(def app (atom nil))

(defn- validate-project-config!
  [conf]
  (let [forbidden-keys [:api]]
    (doseq [k forbidden-keys]
      (when (contains? conf k)
        (let [ex (ex-info "Forbiden key in project config!" {:key k})]
          (log/error ::forbidden-config-key
                     (.getMessage ex)
                     {:project-config conf
                      :error (ex->map ex)})
          (throw ex))))))

(defn- get-config
  []
  (let [profile (or (some-> "BBOTISCAF_PROFILE"
                            System/getenv
                            str/lower-case
                            keyword)
                    :aws)
        bbotiscaf-config (read-config (io/resource "bbotiscaf-resources/config.edn") {:profile profile})
        project-config   (try (read-config (io/resource "config.edn") {:profile profile})
                              (catch Exception ex
                                (log/warn ::no-config-file
                                          "No 'config.edn' file!"
                                          {:error (ex->map ex)})
                                {}))]
    (validate-project-config! project-config)
    (let [config (merge bbotiscaf-config project-config)]
      (log/info ::config-loaded
                "Configuration loaded"
                {:profile profile
                 :bbotiscaf-config bbotiscaf-config
                 :project-config project-config
                 :full-config config})
      config)))

(defn startup!
  []
  (let [config (get-config)]
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
  [_ sym]
  (log/info ::apply-api-fn
            "Applying :api/fn %s... %s" sym (ns-resolve (symbol "bbotiscaf.impl.e2e") 'request)
            {:sym sym})
  (find-var sym))

(defmethod ig/init-key :db/conn
  [_ conn-str]
  (let [schema (merge (get-bbotiscaf-schema) (get-project-schema))
        opts {:validate-data? true
              :closed-schema? true
              :auto-entity-time? true}]
    (log/info ::apply-db-conn
              "Applying :db/conn %s...\nSchema:\t%s" conn-str schema
              {:conn-str conn-str
               :schema schema
               :opts opts})
    (d/get-conn conn-str schema #_opts)))

