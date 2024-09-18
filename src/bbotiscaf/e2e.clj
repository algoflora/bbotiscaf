(ns bbotiscaf.e2e
  (:require
    [bbotiscaf.impl.e2e.flow :as impl]))


(defmacro flow-pipeline
  {:style/indent [1 :form :form]
   :clj-kondo/lint-as 'clojure.test/deftest}
  [& args]
  `(impl/flow-pipeline ~@args))
