(ns bbotiscaf.misc
  (:require
    [babashka.fs :as fs]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
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


(defmulti remove-nils (fn [x]
                        (cond
                          (record? x) :default
                          (map? x)    :map
                          (vector? x) :vec
                          :else       :defalut)))


(defmethod remove-nils :map
  [m]
  (into {} (map (fn [[k v]]
                  (if (nil? v) nil [k (remove-nils v)])) m)))


(defmethod remove-nils :vec
  [v]
  (filterv some? (map #(if (some? %) (remove-nils %) nil) v)))


(defmethod remove-nils :default
  [x]
  (identity x))


(remove-nils [1 2 nil {:a 5 :b nil :c [1 2 nil 3]}])


(defmacro do-nanos
  [& body]
  `(let [~'t0 (System/nanoTime)]
     ~@body
     (- (System/nanoTime) ~'t0)))


(defmacro do-nanos*
  [& body]
  `(let [~'t0 (System/nanoTime)
         ~'r (do ~@body)]
     {:result ~'r
      :nanos (- (System/nanoTime) ~'t0)}))


(defn- char-range
  [lo hi]
  (range (int lo) (inc (int hi))))


(def hex
  (map char (concat (char-range \a \f)
                    (char-range \0 \9))))


(def alpha-numeric
  (map char (concat (char-range \a \z)
                    (char-range \A \Z)
                    (char-range \0 \9))))


(defn- create-generator
  [chars]
  (fn [num]
    (apply str (take num (repeatedly #(rand-nth chars))))))


(defn generate-hex
  [num]
  ((create-generator hex) num))


(defn generate-alpha-numeric
  [num]
  ((create-generator alpha-numeric) num))


(defn user->str
  [user]
  (let [username (:user/username user)]
    (if username
      (str "@" username)
      (str "id" (:user/id user)))))
