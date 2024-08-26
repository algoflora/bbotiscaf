(ns bbotiscaf.misc
  (:require
    [aero.core :refer [read-config]]
    [babashka.fs :as fs]
    [bbotiscaf.impl.config :refer [profile]]
    [clojure.java.io :as io]
    [clojure.stacktrace :as st]
    [clojure.walk :refer [postwalk]]
    [malli.core :as m]
    [taoensso.timbre :as log]))


(defn ex->map
  [ex]
  {:cause      (ex-cause ex)
   :stacktrace (with-out-str (st/print-stack-trace ex))})


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


(m/=> remove-nils [:-> :map :map])


(defn remove-nils
  [in-map]
  (postwalk (fn [item] (if (map? item) (into {} (filter #(-> % val some?) item)) item)) in-map))
