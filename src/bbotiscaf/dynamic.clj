(ns bbotiscaf.dynamic
  (:require
    [datalevin.core :as d]))


(def ^:dynamic *dtlv* nil)


(defn dtlv
  []
  (d/db *dtlv*))


(def ^:dynamic *user* nil)

(def ^:dynamic *upd* nil)

(def ^:dynamic *msg* nil)
