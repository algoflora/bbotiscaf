(ns bbotiscaf.dynamic
  (:require
    [babashka.pods :refer [load-pod]]))


(let [time-str (with-out-str (time (load-pod 'huahaiy/datalevin "0.9.10")))]
  (println "Datalevin Pod loaded. %s" time-str))


(require '[pod.huahaiy.datalevin :refer [db]])

(def ^:dynamic *dtlv* nil)


(defn dtlv
  []
  (db *dtlv*))


(def ^:dynamic *user* nil)

(def ^:dynamic *upd* nil)

(def ^:dynamic *msg* nil)
