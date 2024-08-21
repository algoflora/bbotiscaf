(ns bbotiscaf.aws
  (:require [babashka.fs :as fs]
            [malli.core :as m]
            [clojure.string :as str]
            [bbotiscaf.spec.core :as spec]
            [bbotiscaf.blambda.api :refer [build-all]]
            [bbotiscaf.blambda.api.terraform :refer [write-config apply!]]))

(defn- get-tree
  [dir]
  (->> (fs/glob dir "**.*")
       (filter #(boolean (re-find #".*[^~#]$" (fs/extension %))))))

(def default-opts
  {:bb-arch "arm64"
   :bb-version "1.3.186"
   :deps-layer-name "deps-layer"
   :deps-path "./bb.edn"
   :tfstate-bucket "bbotiscaf"
   :lambda-handler "main/handler"
   :lambda-env-vars []
   :lambda-memory-size 512
   ;; :lambda-name
   ;; :cluster
   :datalevin-version "0.9.10"
   :lambda-runtime "provided.al2023"
   :lambda-timeout 5
   :runtime-layer-name "blambda-layer"
   :source-dir "src"
   :source-files (into [] (concat (get-tree "./src") (get-tree "./resources")))
   :target-dir "target"
   :tf-config-dir "."
   :tf-module-dir "modules"
   :work-dir ".work"})

(m/=> deploy! [:=> [:cat spec/user-opts] :nil])
(defn deploy!
  [opts]
  (let [opts (merge default-opts opts)]
    (build-all opts)
    (write-config opts)
    (apply! opts)))
