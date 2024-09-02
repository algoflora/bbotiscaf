(ns bbotiscaf.dynamic
  (:require
    [babashka.pods :refer [load-pod]]))


(load-pod 'huahaiy/datalevin "0.9.10")
(require '[pod.huahaiy.datalevin :refer [db]])

(def ^:dynamic *dtlv* nil)


(defn dtlv
  []
  (db *dtlv*))


(def ^:dynamic *user* nil)

(def ^:dynamic *upd* nil)

(def ^:dynamic *msg* nil)
