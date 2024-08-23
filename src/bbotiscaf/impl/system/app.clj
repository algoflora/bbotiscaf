(ns bbotiscaf.impl.system.app
  (:require [taoensso.timbre :as log]
            [integrant.core :as ig]))

(defonce ^:private app (atom nil))

(defn app-set? [] (some? @app))

(defn get-app [] @app)

(defn set-app! [data]
  (when (app-set?)
    (log/error ::app-reassign-attempt
               "Attempt to reassign app state!")
    (throw (ex-info  "Attempt to reassign app state!" {:incoming-data data})))
  (reset! app data))

(def bot-token (delay (:bot/token @app)))

(def api-fn (delay (:api/fn @app)))

(def db-conn (delay (:db/conn @app)))

(def handler-main (delay (:handler/main @app)))\

(def handler-namespaces (delay (:handler/namespaces @app)))

(defn shutdown!
  []
  (ig/halt! @app))
