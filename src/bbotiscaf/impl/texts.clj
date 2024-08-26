(ns bbotiscaf.impl.texts
  (:require [bbotiscaf.impl.system.app :as app]
            [bbotiscaf.misc :refer [read-resource-dir throw-error]]))

(def ^:private texts (into {} (->> (read-resource-dir "texts")
                                   (map #(into {} [%]))
                                   (apply merge))))

(defmulti txti (fn [_ path & _] (seqable? path)))

(defmethod txti false
  [lang path & args]
  (apply txti lang (vector path) args))

(defmethod txti true
  [lang path & args]
  (let [path# (vec path)
        lang-map (get-in texts path#)]
    (when (not (map? lang-map))
      (throw-error ::no-text-map-on-path
                   "Not a map in texts by given path!"
                   {:path path :lang-map lang-map}))
    (apply format
           (or ((keyword lang) lang-map) (@app/default-language-code lang-map))
           args)))
