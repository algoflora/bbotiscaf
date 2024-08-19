(ns bbotiscaf.impl.system
  (:require [integrant.core :as ig]
            [bbotiscaf.misc :refer [ex->map]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [aero.core :refer [read-config]]
            [taonesso.timbre :as log]))

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
                    :default)
        default-config (read-config (io/resource "default-config.edn") {:profile profile})
        project-config (try (read-config (io/resource "config.edn") {:profile profile})
                            (catch Exception _ {}))]
    (validate-project-config! project-config)
    (let [config (merge default-config project-config)]
      (log/info "Configuration loaded" {:profile profile
                                        :default-config default-config
                                        :project-config project-config
                                        :full-config config})
      config)))

(defn startup!
  []
  (let [config (get-config)]
    (reset! app (ig/init config))))

(defn shutdown!
  []
  (ig/halt! @app))
