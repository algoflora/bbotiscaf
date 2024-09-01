(ns bbotiscaf.e2e
  (:require
    [bbotiscaf.impl.e2e.flow :as impl]))


(defn flow
  [flow-name ?data-from-flow blueprint]
  (impl/flow flow-name ?data-from-flow blueprint))
