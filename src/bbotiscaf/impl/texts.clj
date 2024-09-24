(ns bbotiscaf.impl.texts
  (:require
    [bbotiscaf.impl.system.app :as app]
    [bbotiscaf.misc :refer [read-resource-dir]]))


(def ^:private texts
  (into {} (->> (read-resource-dir "texts")
                (map #(into {} [%]))
                (apply merge))))


(defmulti txti (fn [_ path & _] (seqable? path)))


(defmethod txti false
  [lang path & args]
  (apply txti lang (vector path) args))


(defmethod txti true
  [lang path & args]
  (let [[path# form] (if (-> path last int?)
                       [(->> path (drop-last 1) vec) (last path)]
                       [(vec path) 0])
        lang-map (get-in texts path#)]
    (when (not (map? lang-map))
      (throw (ex-info "Not a map in texts by given path!"
                      {:event ::no-text-map-on-path :path path :lang-map lang-map})))
    (apply format (or ((keyword lang) lang-map) (@app/default-language-code lang-map))
           ;; (let [forms (or ((keyword lang) lang-map) (@app/default-language-code lang-map))]
           ;;   (if (vector? forms)
           ;;     (nth forms (max form (-> forms count dec)))
           ;;     forms))
           args)))
