(ns bbotiscaf.impl.callback
  (:require
    [bbotiscaf.dynamic :refer [*dtlv* dtlv *user*]]
    [bbotiscaf.impl.system.app :as app]
    ;; [bbotiscaf.misc :refer [do-nanos do-nanos*]]
    [bbotiscaf.spec.model :as spec.mdl]
    [datalevin.core :as d]
    [malli.core :as m]
    [taoensso.timbre :as log]))


;; (def require-namespaces
;;   (delay
;;     (let [namespaces  (app/handler-namespaces)

;;           {:keys [result nanos]}
;;           (do-nanos* (into {}
;;                            (map #(vector (keyword %) (* (do-nanos (require %)) 0.000001))
;;                                 (conj namespaces 'bbotiscaf.handler))))

;;           time-millis (* nanos 0.000001)]
;;       (log/info ::require-namespaces
;;                 "Required namespaces (%.3f msec): %s" time-millis (str namespaces)
;;                 {:namespaces result
;;                  :time-millis time-millis}))))


(defn callbacks-count
  []
  (first (d/q '[:find  [(count ?cb)]
                :where [?cb :callback/uuid]] (dtlv))))


(m/=> set-callback
      [:function
       [:=> [:cat spec.mdl/User :symbol [:or :nil :map]] :uuid]
       [:=> [:cat spec.mdl/User :symbol [:or :nil :map] [:boolean {:optional true}]] :uuid]
       [:=> [:cat
             spec.mdl/User
             :symbol
             [:or :nil :map]
             [:boolean {:optional true}]
             [:uuid {:optional true}]]
        :uuid]])


(defn set-callback
  ([user f args]
   (set-callback user f args false))
  ([user f args is-service]
   (set-callback user f args is-service (java.util.UUID/randomUUID)))
  ([user f args is-service uuid]
   (let [args (or args {})]
     (d/transact! *dtlv* [(cond-> {:callback/uuid uuid
                                   :callback/function f
                                   :callback/arguments args
                                   :callback/is-service is-service
                                   :callback/user [:user/uuid (:user/uuid user)]})])
     ;; TODO: possible perfomance leak
     (log/debug ::callback-created
                "Callback created"
                {;; :callbacks-count (callbacks-count)
                 :callback        (ffirst (d/q '[:find (pull ?cb [*])
                                                 :in $ ?uuid
                                                 :where [?cb :callback/uuid ?uuid]]
                                               (dtlv) uuid))})
     uuid)))


(m/=> delete [:=> [:cat spec.mdl/User :int] :nil])


(defn delete
  [user mid]
  (log/debug ::callbacks-dtlv {:user user :mid mid :dtlv (dtlv)
                               :datoms (d/datoms (dtlv) :eav)})
  (let [db-ids-to-retract (d/q '[:find ?cb
                                 :in $ ?uid ?mid
                                 :where
                                 [?cb :callback/message-id ?mid]
                                 [?cb :callback/user [:user/id ?uid]]]
                               (dtlv) (:user/id user) mid)]
    (log/debug ::callbacks-retraction-transact {})
    (d/transact! *dtlv* (mapv #(vector :db/retractEntity (first %)) db-ids-to-retract))
    ;; TODO: possible perfomance leak
    (log/debug ::callbacks-retracted
               "%d Callbacks retracted by 'delete'" (count db-ids-to-retract)
               {:message-id      mid
                :retracted-count (count db-ids-to-retract)
                :to-retract      db-ids-to-retract
                ;; :callbacks-count (callbacks-count)
                })))


(m/=> set-new-message-ids [:=> [:cat spec.mdl/User [:or :int :nil] [:vector :uuid]] :nil])


(defn set-new-message-ids
  [user mid uuids]
  (let [uuids-to-retract (apply disj
                                (set (mapv first (d/q '[:find ?uuid
                                                        :in $ ?uid ?mid
                                                        :where
                                                        [?cb :callback/user [:user/id ?uid]]
                                                        [?cb :callback/message-id ?mid]
                                                        [?cb :callback/uuid ?uuid]
                                                        #_(not [?cb :callback/uuid ?uuids])]
                                                      ;; TODO: Fix Datalevin with not and collections
                                                      (dtlv) (:user/id user) mid)))
                                (set uuids))]
    (d/transact! *dtlv* (mapv #(vector :db/retractEntity [:callback/uuid %]) uuids-to-retract))
    (d/transact! *dtlv* (mapv #(into {} [[:callback/uuid %] [:callback/message-id mid]]) uuids))
    (log/debug ::set-new-message-ids
               "New message ids set to %d Callbacks" (count uuids)
               {:user user
                :message-id mid
                :callback-uuids uuids
                :retracted-callbacks-uuids uuids-to-retract
                ;; :final-callbacks-count (callbacks-count)
                })))


(m/=> load-callback [:-> :uuid spec.mdl/Callback])


(defn- load-callback
  [uuid]
  (let [callback (d/pull (dtlv) '[* {:callback/user [*]}] [:callback/uuid uuid])]
    (when (nil? callback)
      (throw (ex-info "Callback not found!"
                      {:event ::callback-not-found-error
                       :uuid uuid})))
    (log/debug ::callback-loaded "Callback loaded" {:callback callback})
    (when (not= (:user/id *user*) (-> callback :callback/user :user/id))
      (throw (ex-info "Wrong User attempt to load Callback!"
                      {:event ::wrong-user-callback-call-error
                       :user *user*
                       :callback callback})))
    callback))


(m/=> check-handler! [:-> spec.mdl/User :nil])


(defn check-handler!
  [user]
  (let [user-callback (-> user :user/uuid load-callback)
        main-handler  (app/handler-main)]
    (when-not (or (= main-handler (:callback/function user-callback))
                  (not-empty (:callback/arguments user-callback)))
      (set-callback user main-handler nil false (:user/uuid user)))))


(m/=> call-func [:=> [:cat :symbol :map] :any])


(defn call-func
  [func args]
  (log/debug ::call-func-1 {})
  ((requiring-resolve func) args)
  (log/debug ::call-func-2 {}))


(m/=> call [:function
            [:=> [:cat :uuid] :any]
            [:=> [:cat :uuid :map] :any]])


(defn call
  ([uuid] (call uuid {}))
  ([uuid args]
   (let [callback (load-callback uuid)
         func (:callback/function callback)
         args (merge (:callback/arguments callback) args)]
     (when-not (true? (:callback/is-service callback))
       (check-handler! *user*))
     (call-func func args))))
