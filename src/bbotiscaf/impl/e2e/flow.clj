 (ns bbotiscaf.impl.e2e.flow
   (:require
     [bbotiscaf.dynamic :refer [*dtlv* dtlv]]
     [bbotiscaf.impl.e2e.client :as cl]
     [bbotiscaf.impl.e2e.dummy :as dum]
     [bbotiscaf.impl.system :as sys]
     [bbotiscaf.impl.system.app :as app]
     [bbotiscaf.misc :refer [throw-error]]
     [bbotiscaf.spec.blueprint :as spec.bp]
     [bbotiscaf.spec.commons :refer [Regexp]]
     [bbotiscaf.spec.telegram :as spec.tg]
     [clojure.test :refer [is testing]]
     [clojure.walk :refer [postwalk]]
     [malli.core :as m]
     [pod.huahaiy.datalevin :as d]))


(defn- create-data-query
  [attrs]
  `[:find [(~'pull ~'?e [~'*]) ...]
    :where
    (~'or ~@(map (fn [a] `[~'?e ~a]) attrs))])


(m/=> str?->re [:-> [:or :string Regexp] Regexp])


(defn- str?->re
  [re?]
  (if (string? re?) (re-pattern (java.util.regex.Pattern/quote re?)) re?))


(m/=> get-message [:=> [:cat spec.tg/User [:maybe [:int {:min 1}]]] spec.tg/Message])


(defn- get-message
  [dummy num?]
  (if (pos-int? num)
    (as-> num? $
          (dum/get-last-messages dummy $ false)
          (reverse $) (vec $) (get $ (dec num?)))
    (dum/get-first-message dummy)))


(m/=> send-text [:=> [:cat spec.tg/User spec.bp/SendTextBlueprintEntry] :any])


(defn- send-text
  ([dummy text] (send-text dummy text []))
  ([dummy text entities]
   (cl/send-text dummy text entities)))


(m/=> click-btn [:=> [:cat spec.tg/User spec.bp/ClickBtnBlueprintEntry] :any])


(defn- click-btn
  ([dummy btn-re] (click-btn dummy nil btn-re))
  ([dummy num? btn-re]
   (let [msg (get-message dummy num?)]
     (cl/click-btn dummy msg (str?->re btn-re)))))


(defmulti -check-message (fn [_ arg] (type arg)))


(defmethod -check-message java.lang.String
  [{:keys [text caption] :as msg} exp]
  (testing "text or caption"
    (cond
      (and (some? text) (nil? caption)) (is (= exp text))
      (and (some? caption) (nil? text)) (is (= exp caption))
      :else (throw-error ::text-and-caption
                         ":text and :caption in same Message!"
                         {:message msg}))))


(defmethod -check-message java.util.regex.Pattern
  [{:keys [text caption]} exp]
  (testing "text or caption regex"
    (is (or (some? (re-find exp text))
            (some? (re-find exp caption))))))


(defmethod -check-message clojure.lang.PersistentVector
  [msg exp]
  (testing "buttons"
    (doseq [[row-idx row] (map-indexed vector exp)]
      (doseq [[col-idx data] (map-indexed vector row)]
        (let [re  (str?->re data)
              btn (get-in (-> msg :reply_markup :inline_keyboard) [row-idx col-idx])]
          (is (some? (re-find re (:text btn)))))))))


(defn- -check-message-entities
  [msg exp]
  (testing "text or caption entities"
    (let [exp (vec exp)]
      (is (or (= exp (:entities msg))
              (= exp (:caption_entities msg)))))))


(defmethod -check-message clojure.lang.PersistentList
  [msg exp]
  (-check-message-entities msg exp))


(defmethod -check-message clojure.lang.PersistentList$EmptyList
  [msg exp]
  (-check-message-entities msg exp))


(m/=> check-msg [:=> [:cat spec.tg/User spec.bp/CheckMessageBlueprintEntry] :nil])


(defn- check-msg
  [dummy & args]
  (let [[num args] (if (-> args first pos-int?) [(first args) (rest args)] [nil args])
        msg (get-message dummy num)]
    (testing "check-message"
      (doseq [arg args]
        (-check-message msg arg)))))


;; TODO: Find out what the fuck!
;; (m/=> apply-blueprint [:-> spec.bp/Blueprint :nil])


(defn- apply-blueprint
  [blueprint]
  (when (not-empty blueprint)
    (let [key   (-> blueprint first namespace keyword)
          dummy (if (dum/exists? key) (-> key dum/get-by-key :dummy) (-> key dum/new :dummy))
          func  (->> blueprint first name (symbol "bbotiscaf.impl.e2e.flow") find-var)
          args  (->> blueprint rest (take-while #(not (spec.bp/is-ns-kw? %))))]
      (apply func dummy args)
      (apply-blueprint (drop (+ 1 (count args)) blueprint)))))


(defonce flows-data (atom {}))


(defn- negate-db-ids
  [data]
  (println "NEGATE" data)
  (let [res (postwalk #(if (and (map? %) (contains? % :db/id)) (assoc % :db/id (- (:db/id %))) %)
                      data)]
    (println "NEGATED" res)
    res))


(defn flow
  [name data-from-flow blueprint]
  (let [{:keys [db-data dummies]} (if (some? data-from-flow) (data-from-flow @flows-data) {})]
    (testing name
      (System/setProperty "bbotiscaf.test.uuid" (str (java.util.UUID/randomUUID)))
      (sys/startup!)
      (dum/restore dummies)
      (let [final-db-data
            (binding [*dtlv* @app/db-conn]
              (clojure.pprint/pprint ["DB_DATA" name db-data *dtlv* (d/transact! *dtlv* db-data) (d/q '[:find [(pull ?e [*]) ...]
                     :where [?e]]
                   (dtlv))])
              (apply-blueprint blueprint)
              (d/q '[:find [(pull ?e [*]) ...]
                     :where [?e]]
                   (dtlv)))
            final-dummies (dum/dump-all)]
        (dum/clear-all)
        (sys/shutdown!)
        (System/clearProperty "bbotiscaf.test.uuid")
        (swap! flows-data assoc (keyword name) {:db-data (negate-db-ids final-db-data)
                                                :dummies final-dummies})))))

