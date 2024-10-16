(ns bbotiscaf.impl.system.app)


(defonce ^:private app (atom nil))


(defn app-set?
  []
  (some? @app))


(defn get-app
  []
  @app)


(defn set-app!
  [data]
  (when (app-set?)
    (throw (ex-info "Attempt to reassign app state!"
                    {:event ::app-reassign-attempt-error
                     :incoming-data data})))
  (reset! app data))


(defn update-project-config
  [f]
  (swap! app update :project/config f))


(defn clear-app!
  []
  (reset! app nil))


(defn db-conn
  []
  (:db/conn @app))


(def bot-token (delay (:bot/token @app)))

(def bot-roles (delay (:bot/roles @app)))


(defn default-language-code
  []
  (:bot/default-language-code @app))


(def api-fn (delay (:api/fn @app)))


(defn project-config
  []
  (:project/config @app))


(defn handler-main
  []
  (:handler/main @app))


(defn handler-payment
  []
  (:handler/payment @app))


(defn handler-namespaces
  []
  (:handler/namespaces @app))
