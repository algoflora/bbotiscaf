(ns bbotiscaf.impl.config
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [malli.core :as m]
            [taoensso.timbre :as log]
            [aero.core :refer [read-config]]
            [bbotiscaf.spec.app :as spec.app]))

(def ^:private profile (some-> "BBOTISCAF_PROFILE"
                               System/getenv
                               str/lower-case
                               keyword))

(m/=> get-bbotiscaf-config [:=> [:cat] spec.app/bbotiscaf-config-schema])
(defn- get-bbotiscaf-config
  []
  (read-config (io/resource "bbotiscaf-resources/config.edn") {:profile profile}))

(m/=> get-project-config [:=> [:cat] spec.app/project-config-schema])
(defn- get-project-config
  []
  (read-config (io/resource "config.edn") {:profile profile}))

(m/=> get-config [:=> [:cat] spec.app/config-schema])
(defn get-config
  []
  (let [bbotiscaf-config (get-bbotiscaf-config)
        project-config   (get-project-config)
        config (merge bbotiscaf-config project-config)]
    (log/info ::config-loaded
              "Configuration loaded"
                {:profile profile
                 :bbotiscaf-config bbotiscaf-config
                 :project-config project-config
                 :full-config config})
    config))
