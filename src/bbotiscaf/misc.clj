(ns bbotiscaf.misc
  (:require
    [babashka.fs :as fs]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.walk :refer [postwalk]]
    [malli.core :as m]))


(defn dbg
  [x]
  (println "DBG\t" x) x)


(defn validate!
  [schema value]
  (when-let [explanation (m/explain schema value)]
    (throw (ex-info  "Validation failed!"
                     {:event :validateion-error
                      :explanation explanation}))))


(defn- to-readable
  [path]
  (or (io/resource path) (.toFile path)))


(defn read-resource-dir
  ([path] (read-resource-dir path "**.edn"))
  ([path pattern]
   (some->> (some-> path
                    io/resource
                    (fs/glob pattern)
                    flatten)
            (map #(-> % to-readable slurp edn/read-string))
            (apply merge))))


(m/=> remove-nils [:-> :map :map])


(defn remove-nils
  [in-map]
  (postwalk (fn [item] (if (map? item) (into {} (filter #(-> % val some?) item)) item)) in-map))


(defmacro do-nanos
  [& body]
  `(let [t0 (system-time)]
     ~@body
     (- (system-time) t0)))
