(ns bbotiscaf.logging
  (:require [clojure.string :as str]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]))

(def ^:private lambda-context (atom nil))

(defn set-lambda-context! [context]
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

      (= cnt (+ 2 (count (re-seq #"(?<!%)%[sdfx]" arg2))))
      (process-vargs (conj vargs {}))

      (and (= cnt (+ 3 (count (re-seq #"(?<!%)%[sdfx]" arg2))))
           (map? argn))
      (merge {:event-name arg1
              :message-text (apply format (rest vargs))}
             argn)

      :else (throw (ex-info "Bad log arguments!" {:log-arguments vargs})))))

(defn lambda-stdout-appender []
  {:enabled?   true
   :async?     false
   :min-level  :info
   :rate-limit nil
   :output-fn  :inherit
   :fn         (fn [{:keys [level ?err vargs ?ns-str
                            ?file hostname_ timestamp_ ?line] :as d}]
                 (let [data (process-vargs vargs)]
                   (println (format "%s [%s] <%s:%s:%s> - %s%s%s"
                                    @timestamp_
                                    (-> level name str/upper-case)
                                    @hostname_
                                    (or ?ns-str ?file "?")
                                    (or ?line "?")
                                    (if (:event-name data)
                                      (str (:event-name data)
                                           (if (not-empty (:message-text data))
                                             " - " "")) "")
                                    (if (not-empty (:message-text data))
                                      (:message-text data) "")
                                    (if ?err
                                      (str "\n" (.getStacktrace ?err)) "")
                                    ))))})

(timbre/merge-config! {:appenders {:println (lambda-stdout-appender)}})

(defn inject-lambda-context! []
  (timbre/merge-config!
   {:middleware [#(assoc % :lambda-context @lambda-context)]}))

  


