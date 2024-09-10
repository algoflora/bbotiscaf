(ns bbotiscaf.logging
  (:require
    [babashka.fs :as fs]
    [bbotiscaf.impl.config :as conf]
    [cheshire.core :refer [generate-string]]
    [clojure.stacktrace :as st]
    [clojure.string :as str]
    [clojure.walk :refer [postwalk]]
    [taoensso.timbre :as timbre]))


(defonce ^:private lambda-context (atom nil))


(defn set-lambda-context!
  [context]
  (reset! lambda-context context))


(defn- process-vargs
  [vargs]
  (let [cnt (count vargs)
        [arg1 arg2] vargs
        argn (last vargs)]
    (cond
      (not (or (keyword? arg1) (nil? arg1)))
      (process-vargs (apply conj [nil] vargs))

      (not (or (string? arg2) (nil? arg2)))
      (process-vargs (apply conj [arg1 ""] (rest vargs)))

      (nil? arg2)
      (process-vargs (apply conj [arg1 ""] (drop 2 vargs)))

      (= cnt (+ 2 (count (re-seq #"(?<!%)%(\.\d+)?[sdfx]" arg2))))
      (process-vargs (conj vargs {}))

      (and (= cnt (+ 3 (count (re-seq #"(?<!%)%(\.\d+)?[sdfx]" arg2))))
           (map? argn))
      (merge {:event-name arg1
              :message-text (apply format (rest vargs))}
             argn)

      :else (throw (ex-info "Bad log arguments!" {:log-arguments vargs})))))


(def lambda-stdout-appender
  {:enabled?   (= conf/profile :test)
   :async?     false
   :min-level  :debug
   :rate-limit nil
   :output-fn  :inherit
   :fn         (fn [{:keys [level ?err vargs ?ns-str
                            ?file hostname_ timestamp_ ?line]}]
                 (let [data (process-vargs vargs)]
                   (println (format "%s [%s] <%s:%s:%s> - %s%s"
                                    @timestamp_
                                    (-> level name str/upper-case)
                                    @hostname_
                                    (or ?ns-str ?file "?")
                                    (or ?line "?")
                                    (if (not-empty (:message-text data))
                                      (:message-text data)
                                      (str (:event-name data)))
                                    (if ?err
                                      (str "\n" (with-out-str (st/print-stack-trace ?err)))
                                      "")))))})


(defn- check-json
  [data]
  (postwalk #(try (generate-string %)
                  %
                  (catch java.lang.Exception _
                    (str/trimr (prn-str %))))
            data))


(defn- json-prepare
  [event]
  (let [data (check-json (select-keys (merge event (process-vargs (:vargs event)))
                                      [:instant :message-text :event-name :vargs
                                       :?err :?file :?line]))
        vargs (:vargs data)]
    (-> data
        (dissoc :vargs)
        (assoc :data (last vargs)))))


(def lambda-json-println-appender
  {:enabled? (= conf/profile :aws)
   :fn (fn [event]
         (println (generate-string (json-prepare event))))})


(def lambda-json-spit-appender
  {:enabled? (= conf/profile :test)
   :fn (fn [event]
         (spit "logs.json" (str (generate-string (json-prepare event)) "\n") :append true))})


(fs/delete-if-exists "logs.json")


(fs/delete-if-exists "logs.edn")


(timbre/merge-config! {:min-level :info
                       :appenders (merge {:println lambda-stdout-appender
                                          :json-file lambda-json-spit-appender
                                          :json-print lambda-json-println-appender})})


(defn inject-lambda-context!
  []
  (timbre/merge-config!
    {:middleware [#(assoc % :lambda-context @lambda-context)]}))
