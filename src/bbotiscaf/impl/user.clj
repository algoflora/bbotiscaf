(ns bbotiscaf.impl.user
  (:require
    [babashka.pods :refer [load-pod]]
    [bbotiscaf.dynamic :refer [*dtlv* dtlv]]
    [bbotiscaf.impl.system.app :as app]
    [taoensso.timbre :as log]))


(load-pod 'huahaiy/datalevin "0.9.10")
(require '[pod.huahaiy.datalevin :as d])


(defn get-list
  []
  (d/q '[:find (pull ?u [*])
         :where [?u :user/username]] (dtlv)))


(defn load-by-username
  [username]
  (d/pull (dtlv) '[*] [:user/username username]))


(defn- load-by-udata
  [udata]
  (d/pull (dtlv) '[*] [:user/id (:id udata)]))


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
    (log/debug ::user-renewed
               "User renewed"
               {:user user})
    user))


(defn- create
  [udata]
  (let [uuid (java.util.UUID/randomUUID)]
    (d/transact! *dtlv* [(->> {:db/id -1
                               :user/uuid uuid
                               :user/id (:id udata)
                               :user/username (:username udata)
                               :user/first-name (:first_name udata)
                               :user/last-name (:last_name udata)
                               :user/language-code (:language_code udata)}
                              (filter #(-> % second some?))
                              (into {}))
                         {:callback/uuid uuid
                          :callback/function @app/handler-main
                          :callback/arguments {}
                          :callback/user {:db/id -1}
                          :callback/is-service false}]))
  (let [user (load-by-udata udata)]
    (log/debug ::user-created
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
    (log/debug ::user-loaded
               "User was loaded: %s" user
               {:user user})
    user))


(defn set-msg-id
  [user msg-id]
  (d/transact! *dtlv* [{:user/id (:user/id user)
                        :user/msg-id msg-id}])
  (log/debug ::set-msg-id
             "User's msg-id was set to %d" msg-id
             {:user user :msg-id msg-id}))
