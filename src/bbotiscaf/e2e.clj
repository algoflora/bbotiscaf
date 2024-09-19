(ns bbotiscaf.e2e
  (:require
    [bbotiscaf.impl.e2e.flow :as impl]))


(defmacro defflow
  {:style/indent [1]}
  [& args]
  `(impl/defflow ~@args))


(defmacro flow-pipeline
  {:style/indent [1]
   :clj-kondo/lint-as 'clojure.core/def}
  [& args]
  `(impl/flow-pipeline ~@args))
