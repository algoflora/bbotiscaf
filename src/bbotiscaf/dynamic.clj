(ns bbotiscaf.dynamic
  (:require
    [babashka.pods :refer [load-pod]]
    [bbotiscaf.logging]
    [bbotiscaf.misc :refer [do-nanos]]
    [taoensso.timbre :as log]))


(let [time-millis (* (do-nanos (load-pod 'huahaiy/datalevin "0.9.10")) 0.000001)]
  (log/info ::datalevin-pod-loaded
            "Datalevin Pod loaded in %.3f msec" time-millis
            {:time-millis time-millis}))


(require '[pod.huahaiy.datalevin :refer [db]])

(def ^:dynamic *dtlv* nil)


(defn dtlv
  []
  (db *dtlv*))


(def ^:dynamic *user* nil)

(def ^:dynamic *upd* nil)

(def ^:dynamic *msg* nil)
