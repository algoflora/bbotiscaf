(ns bbotiscaf.core
  (:require [malli.core :as m]
            [malli.util :as mu]
            [malli.instrument :as mi]
            [bbotiscaf.spec.core :as spec]))

(m/=> handler [:=> [:cat spec/request-schema] :nil])
(defn handler
  [req]
  (println "REQ:" req (type req)))

(mi/instrument!)
 
;(handler 1)
