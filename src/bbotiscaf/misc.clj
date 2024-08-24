(ns bbotiscaf.misc
  (:require [malli.core :as m]
            [clojure.java.io :as io]
            [aero.core :refer [read-config]]
            [babashka.fs :as fs]
            [bbotiscaf.impl.config :refer [profile]]
            [taoensso.timbre :as log]))

(defn ex->map
  [ex]
  {:cause      (ex-cause ex)
   :stacktrace (.getStackTrace ex)})

(defn throw-error
  [kw text data]
  (let [ex (ex-info text data)]
    (log/error kw text (merge (ex->map ex) data))
    (throw ex)))

(defn validate!
  [schema value]
  (when-let [explanation (m/explain schema value)]
    (throw-error ::validation-failed
                 "Validation failed!"
                 {:explanation explanation})))

(defn read-resource-dir
  ([path] (read-resource-dir path "**.edn"))
  ([path pattern]
   (some->> (some-> path
                    io/resource
                    (fs/glob pattern)
                    flatten)
            (map #(-> % slurp (read-config profile)))
            (apply merge))))
