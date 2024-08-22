(ns bbotiscaf.impl.user
  (:require [taoensso.timbre :as log]
            [bbotiscaf.impl.callback :as clb]
            [bbotiscaf.vars :refer [*dtlv*]]
            [pod.huahaiy.datalevin :as d]))

(defn get-list
  []
  (d/q '[:find (pull ?u [*])
         :where [?u :user/username]] @*dtlv*))

(defn load-by-username
  [username]
  (d/pull @*dtlv* '[*] [:user/username username]))

(defn- load-by-udata
  [udata]
  (d/pull @*dtlv* '[*] [:user/id (:id udata)]))

(defn- is-new-udata?
  [udata user]
  (or (not= (:username udata) (:user/username user))
      (not= (:first_name udata) (:user/first-name user))
      (not= (:last_name udata) (:user/last-name user))
      (not= (:language_code udata) (:user/language-code user))))

(defn- renew
  [udata]
  (d/transact! *dtlv* [(->> {:user/id (:id udata)
                             :user/username (:username udata)
                             :user/first-name (:first_name udata)
                             :user/last-name (:last_name udata)
                             :user/language-code (:language_code udata)}
                            (filter #(-> % second some?))
                            (into {}))])
  (let [user (load-by-udata udata)]
    (log/info ::user-renewed
              "User renewed"
              {:user user})
    user))


(defn- create
  [udata]
  (d/transact! *dtlv* [(->> {:user/uuid (java.util.UUID/randomUUID)
                             :user/id (:id udata)
                             :user/username (:username udata)
                             :user/first-name (:first_name udata)
                             :user/last-name (:last_name udata)
                             :user/language-code (:language_code udata)}
                            (filter #(-> % second some?))
                            (into {}))])
  (let [user (load-by-udata udata)]
    (log/info ::user-created
              "New User created: %s" user
              {:user user})
    user))

(defn load-or-create
  [udata]
  (let [user? (load-by-udata udata)
        user  (cond
                (nil? user?)                (create udata)
                (is-new-udata? udata user?) (renew udata)
                :else                       user?)]
    (log/info ::user-loaded
              "User was loaded: %s" user
              {:user user})
    user))

(defn set-msg-id
  [user msg-id]
  (d/transact! *dtlv* [{:user/id (:user/id user)
                        :user/msg-id msg-id}])
  (log/info ::set-msg-id
            "User's msg-id was set to %d" msg-id
            {:user user :msg-id msg-id}))
