(ns bbotiscaf.impl.system
  (:require
    [bbotiscaf.impl.config :as conf]
    [bbotiscaf.impl.e2e]
    [bbotiscaf.impl.errors :refer [handle-error]]
    [bbotiscaf.impl.system.app :as app]
    [bbotiscaf.misc :refer [read-resource-dir]]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.walk :refer [postwalk]]
    [integrant.core :as ig]
    [malli.instrument :as mi]
    [taoensso.timbre :as log]))


(require '[pod.huahaiy.datalevin :as d])


(defn- get-bbotiscaf-schema
  []
  (-> "bbotiscaf-resources/schema.edn"
      io/resource
      slurp
      edn/read-string))


(defmethod ig/init-key :api/fn
  [_ symbol]
  (log/debug ::init-api-fn
             "Applying :api/fn %s..." symbol
             {:symbol symbol})
  (find-var symbol))


(defmethod ig/init-key :db/conn
  [_ conn-str]
  (let [schema (merge (get-bbotiscaf-schema) (read-resource-dir "schema"))
        opts   {:validate-data? true
                :closed-schema? true
                :auto-entity-time? true}
        cn-str (or conn-str (format "/tmp/bbotiscaf-test/%s" (str (java.util.UUID/randomUUID))))]
    (log/debug ::init-db-conn
               "Applying :db/conn %s..." cn-str
               {:conn-str cn-str
                :schema schema
                :opts opts})
    (d/get-conn cn-str schema #_opts)))


(defmethod ig/halt-key! :db/conn
  [_ conn]
  (log/debug ::halt-db-conn
             "Halting :db/conn...")
  (d/close conn))


(defmethod ig/init-key :bot/token
  [_ token]
  (log/debug ::init-bot-token
             "Applying :bot/token %s..." token
             {:token token})
  token)


(defmethod ig/init-key :bot/default-language-code
  [_ code]
  (log/debug ::init-bot-default-language-code
             "Applying :bot/default-language-code %s..." code
             {:code code})
  code)


(defn- load-role
  [roles rs]
  (let [entries ((first rs) roles)]
    (postwalk (fn [x]
                (cond
                  (and (keyword? x) (some #{x} (set rs)))
                  (throw (ex-info "Circular roles dependencies!"
                                  {:event ::circular-roles-error
                                   :role x
                                   :roles roles}))

                  (keyword? x) (load-role roles (conj x rs))
                  :else x))
              entries)))


(defmethod ig/init-key :bot/roles
  [_ roles]
  (into {} (map (fn [[k _]] [k (load-role roles k)]) roles)))


(defmethod ig/init-key :handler/namespaces
  [_ namespaces]
  (log/debug ::init-handler-namespaces
             "Applying :handler/namespaces %s..." namespaces
             {:namespaces namespaces})
  namespaces)


(defmethod ig/init-key :handler/main
  [_ handler]
  (log/debug ::init-handler-main
             "Applying :handler/main %s..." handler
             {:handler handler})
  handler)


(defn startup!
  ([] (startup! {}))
  ([conf]
   (when-not (app/app-set?)
     (Thread/setDefaultUncaughtExceptionHandler
       (reify Thread$UncaughtExceptionHandler
         (uncaughtException
           [_ thread e]
           (handle-error thread e))))
     (mi/instrument!)
     (let [config (conf/get-config)]
       (app/set-app! (ig/init (merge config conf)))
       (log/info ::startup-completed
                 "Startup completed: %s" (app/get-app))))))


(defn shutdown!
  []
  (ig/halt! (app/get-app))
  (app/clear-app!)
  (log/info ::sutdown-completed
            "Shutdown completed!"))
