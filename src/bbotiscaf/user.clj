(ns bbotiscaf.user
  (:require
    [bbotiscaf.dynamic :refer [*user*]]
    [bbotiscaf.impl.callback :as clb]
    [bbotiscaf.impl.system.app :as app]))


(defn has-role?
  ([role] (has-role? role *user*))
  ([role user]
   (boolean (some #{(:user/id user) (:user/username user)} (set (role @app/bot-roles))))))


(defmacro with-role
  {:clj-kondo/lint-as 'clojure.core/when}
  [role & body]
  `(do
     (require '[bbotiscaf.api]
              '[bbotiscaf.button]
              '[bbotiscaf.dynamic])
     (if (bbotiscaf.user/has-role? ~role ~'bbotiscaf.dynamic/*user*)
       (do ~@body)
       (bbotiscaf.api/send-message ~'*user* "⛔ Forbidden! ⛔" [[(bbotiscaf.button/home-btn "🏠 To Main Menu")]]))))


(defn set-handler
  ([func] (set-handler func {}))
  ([func args] (set-handler func args *user*))
  ([func args user]
   (clb/set-callback user func args false (:user/uuid user))))
