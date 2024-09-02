(ns bbotiscaf.dynamic
  (:require
    [babashka.pods :refer [load-pod]]
    [taoensso.timbre :as log]))


(let [time-str (with-out-str (time (load-pod 'huahaiy/datalevin "0.9.10")))]
  (log/info ::datalevin-pod-loaded
            "Datalevin Pod loaded. %s" time-str
            {:elapsed-time (Float/parseFloat (re-find #"\d+.?\d*" time-str))}))


(require '[pod.huahaiy.datalevin :refer [db]])

(def ^:dynamic *dtlv* nil)


(defn dtlv
  []
  (db *dtlv*))


(def ^:dynamic *user* nil)

(def ^:dynamic *upd* nil)

(def ^:dynamic *msg* nil)
