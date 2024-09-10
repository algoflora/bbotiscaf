(ns bbotiscaf.user
  (:require
    [bbotiscaf.dynamic :refer [*user*]]
    [bbotiscaf.impl.system.app :as app]
    [bbotiscaf.api :as api]))


(defn has-role?
  ([role] (has-role? role *user*))
  ([role user]
   (boolean (some #{(:user/id user) (:user/username user)} (set (role @app/bot-roles))))))
