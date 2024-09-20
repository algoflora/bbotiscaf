 (ns bbotiscaf.impl.e2e.flow
   (:require
     [bbotiscaf.impl.e2e.client :as cl]
     [bbotiscaf.impl.e2e.dummy :as dum]
     [bbotiscaf.impl.errors :refer [handle-error]]
     [bbotiscaf.impl.system :as sys]
     [bbotiscaf.impl.system.app :as app]
     [bbotiscaf.spec.blueprint :as spec.bp]
     [bbotiscaf.spec.commons :refer [Regexp]]
     [bbotiscaf.spec.telegram :as spec.tg]
     [clojure.string :as str]
     [clojure.test :refer [is testing]]
     [clojure.walk :refer [postwalk]]
     [malli.core :as m]
     [taoensso.timbre :as log]))


(require '[pod.huahaiy.datalevin :as d])


(m/=> str?->re [:-> [:or :string Regexp] Regexp])


(defn- str?->re
  [re?]
  (if (string? re?) (re-pattern (java.util.regex.Pattern/quote re?)) re?))


(defn- dump
  [dummy]
  (let [key   (-> dummy :username keyword)
        dummy (dum/get-by-key key)]
    (log/info ::dumping-dummy
              "Dumping dummy %s\n%s" key (with-out-str (clojure.pprint/pprint dummy))
              {:dummy dummy})))


(m/=> get-message [:=> [:cat spec.tg/User [:maybe [:int {:min 1}]]] spec.tg/Message])


(defn- get-message
  [dummy ?num]
  (if (pos-int? ?num)
    (as-> ?num $
          (dum/get-last-messages dummy $ false)
          (reverse $) (vec $) (get $ (dec ?num)))
    (dum/get-first-message dummy)))


(m/=> send-text [:=> [:cat spec.tg/User spec.bp/SendTextBlueprintEntryArgs] :any])


(defn- send-text
  ([dummy text] (send-text dummy text []))
  ([dummy text entities]
   (log/debug ::dummy-send-text
              "Dummy %s sendindg message: %s" (-> dummy :username keyword) text
              {:dummy dummy
               :text text})
   (cl/send-text dummy (str text) entities)))


(m/=> click-btn [:=> [:cat spec.tg/User spec.bp/ClickBtnBlueprintEntryArgs] :any])


(defn- click-btn
  ([dummy btn-re] (click-btn dummy nil btn-re))
  ([dummy num? btn-re]
   (let [msg (get-message dummy num?)]
     (log/debug ::dummy-click-btn
                "Dummy %s clicking button '%s' in message '%s'"
                (-> dummy :username keyword) (str btn-re) (or (:text msg) (:caption msg))
                {:dummy dummy
                 :button btn-re
                 :message msg})
     (cl/click-btn dummy msg (str?->re btn-re)))))


(defmulti -check-message (fn [_ arg] (type arg)))


(defmethod -check-message java.lang.String
  [{:keys [text caption] :as msg} exp]
  (testing "text or caption"
    (cond
      (and (some? text) (nil? caption)) (is (= exp text))
      (and (some? caption) (nil? text)) (is (= exp caption))
      :else (throw (ex-info ":text and :caption in same Message!"
                            {:event ::text-and-caption-both-error
                             :message msg})))))


(defmethod -check-message java.util.regex.Pattern
  [{:keys [text caption] :as msg} exp]
  (testing "text or caption regex"
    (is (or (and (some? text) (some? (re-find exp text)))
            (and (some? caption) (some? (re-find exp caption)))))))


(defmethod -check-message clojure.lang.PersistentVector
  [msg exp]
  (let [kbd (-> msg :reply_markup :inline_keyboard)]
    (testing (str "buttons: " exp "in " kbd "\n")
      (is (= (count exp) (count kbd)) "Different rows count!")
      (doseq [[row-idx row] (map-indexed vector exp)]
        (is (= (count (get exp row-idx)) (count row))
            (format "Different columns count in row %d" row-idx))
        (doseq [[col-idx data] (map-indexed vector row)]
          (let [re  (str?->re data)
                btn (get-in kbd [row-idx col-idx])]
            (is (some? (re-find re (:text btn))))))))))


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


(m/=> check-msg [:=> [:cat spec.tg/User spec.bp/CheckMessageBlueprintEntryArgs] :nil])


(defn- check-msg
  [dummy & args]
  (let [[num args] (if (-> args first pos-int?) [(first args) (rest args)] [nil args])
        msg (get-message dummy num)]
    (testing "check-message"
      (doseq [arg args]
        (-check-message msg arg)))))


(defonce vars (atom {}))


(defn- save-var-from-message
  [dummy & args]
  (let [[num args] (if (-> args first pos-int?) [(first args) (rest args)] [nil args])
        msg (get-message dummy num)]
    (swap! vars assoc (first args) (cond->> msg
                                     (contains? msg :caption) :caption
                                     (contains? msg :text) :text
                                     true (re-find (second args))
                                     true second))))


(defn- check-vars
  [_ f k1 k2]
  (let [v1 (k1 @vars)
        v2 (k2 @vars)]
    (testing (format "%s of %s (%s) and %s (%s)" f k1 v1 k2 v2)
      (is (f v1 v2)))))


(defn- check-no-temp-messages
  [dummy]
  (let [m-msg (get-message dummy nil)
        t-msg (get-message dummy 1)]
    (is (= m-msg t-msg))))


;; TODO: Find out what the hell!
;; (m/=> apply-blueprint [:-> spec.bp/Blueprint :nil])


(defn- apply-blueprint
  ([blueprint] (apply-blueprint blueprint 1))
  ([blueprint line]
   (when (not-empty blueprint)
     (let [key   (-> blueprint first namespace keyword)
           dummy (cond
                   (dum/exists? key) (-> key dum/get-by-key :dummy)

                   (= (-> key name (str/split #"\.") first)
                      (-> (app/handler-main) namespace (str/split #"\.") first))
                   nil

                   :else (-> key dum/new :dummy))
           symb  (-> blueprint first name)
           func  (find-var (symbol "bbotiscaf.impl.e2e.flow" symb))
           args  (->> blueprint rest (take-while #(not (qualified-keyword? %))))]
       (testing (format "%4d | <%s/%s %s>\n" line key symb (str/join " " args))
         (apply func dummy args))
       (apply-blueprint (drop (+ 1 (count args)) blueprint) (inc line))))))


(defmulti ^:private sub-flow (fn [_ x & _] (cond (fn? x) :function (vector? x) :vector)))


(defmethod sub-flow :vector
  [_ blueprint]
  ;; TODO: Find out what the hell!
  #_(when-let [error (m/explain spec.bp/Blueprint blueprint)]
    (throw (ex-info "Wrong Blueprint in sub-flow method!"
                    {:event ::wrong-sub-flow-blueprint-error
                     :error-explain error})))
  (apply-blueprint blueprint))


(defmethod sub-flow :function
  [_ f & args]
  (let [blueprint (apply f args)]
    (sub-flow nil blueprint)))


(defn- check-and-close-only-temp
  [dummy & args]
  (let [ns (:username dummy)]
    (sub-flow dummy
              (vec (concat [(keyword ns "check-msg") 1] args
                           [(keyword ns "click-btn") 1 "✖️" (keyword ns "check-no-temp-messages")])))))


(defn- check-and-close-last-temp
  [dummy & args]
  (let [ns (:username dummy)]
    (sub-flow dummy
              (vec (concat [(keyword ns "check-msg") 1] args
                           [(keyword ns "click-btn") 1 "✖️"])))))


(defn- call!
  [_ f & args]
  (apply (find-var f) args))


(defn- println!
  [_ text]
  (println text))


(defn- flow
  [blueprint]
  (try
    (sys/startup!)
    (apply-blueprint blueprint)
    (dum/clear-all)
    (sys/shutdown!)
    (catch Exception ex
      (handle-error ex)
      (throw ex))))


(defonce flows (atom {}))


(defmacro defflow
  [key blueprint]
  {:style/indent [1]}
  `(swap! flows assoc ~key ~blueprint))


(defmacro flow-pipeline
  {:style/indent [1]
   :clj-kondo/lint-as 'clojure.core/def}
  [name & args]
  (let [[h arg] (if (= 2 (count args)) [(first args) (second args)] [nil (first  args)])]
    `(clojure.test/deftest ~name
       (let [~'blueprint (vec (apply concat (mapv #(% @flows) ~arg)))]
         (with-redefs [bbotiscaf.impl.system.app/handler-main
                       (if (some? ~h) (fn [] ~h) bbotiscaf.impl.system.app/handler-main)]
           (flow ~'blueprint))))))
