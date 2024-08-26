(ns bbotiscaf.dynamic
  (:require [pod.huahaiy.datalevin :refer [db]]))

(def ^:dynamic *dtlv* nil)

(defn dtlv [] (db *dtlv*))

(def ^:dynamic *user* nil)

(def ^:dynamic *upd* nil)

(def ^:dynamic *msg* nil)
