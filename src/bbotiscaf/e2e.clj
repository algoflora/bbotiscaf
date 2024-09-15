(ns bbotiscaf.e2e
  (:require
    [bbotiscaf.impl.e2e.flow :as impl]))


(defn get-flow-var
  [k]
  (k @impl/vars))


(defn flow
  [flow-name ?data-from-flow blueprint]
  (impl/flow flow-name ?data-from-flow blueprint))
