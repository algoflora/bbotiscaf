(ns ^:no-doc bbotiscaf.impl.api
  (:require
    [babashka.fs :as fs]
    [babashka.http-client :as http]
    [bbotiscaf.button :as b]
    [bbotiscaf.impl.button :as ib]
    [bbotiscaf.impl.callback :as clb]
    [bbotiscaf.impl.system.app :as app]
    [bbotiscaf.impl.user :as u]
    [bbotiscaf.misc :refer [throw-error]]
    [bbotiscaf.spec.model :as spec.mdl]
    [bbotiscaf.spec.telegram :as spec.tg]
    [cheshire.core :refer [generate-string parse-string]]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [malli.core :as m]
    [taoensso.timbre :as log]))


(defn request
  [method data]
  (let [url  (format "https://api.telegram.org/bot%s/%s" @app/bot-token (name method))
        raw  (http/post url {:headers {:content-type "application/json"}
                             :body (generate-string data)})
        resp (update raw :body #(try (parse-string % true)
                                     (catch Throwable _ %)))]
    (log/debug ::telegram-api-request
               "Telegram API request was sent")
    (log/info ::telegram-api-response
              "Telegram api method %s reponse status %d" method (:status resp)
              {:method method
               :data data
               :response resp})
    (if (-> resp :body :ok)
      (-> resp :body :result)
      (log/warn ::bad-telegram-api-response
                "Telegram API response is not OK! %s" resp
                {:method method
                 :data data
                 :response resp}))))


(defn api-wrap
  [method data]
  (let [api-fn @app/api-fn]
    (log/debug ::calling-api-fn
               "Calling API request function %s..." api-fn
               {:api/fn api-fn
                :method method
                :data data})
    (api-fn method data)))


(defn- check-opt
  [opts opt]
  (boolean (some #{opt} opts)))


(defn- prepare-options-map
  [opts]
  (let [to-edit-msg-id (->> opts (filter number?) (first))]
    (-> {}
        (assoc :temp           (or (check-opt opts :temp) (some? to-edit-msg-id)))
        (assoc :markdown       (check-opt opts :markdown))
        (assoc :to-edit-msg-id to-edit-msg-id))))


(m/=> prepare-keyboard [:=>
                        [:cat
                         [:maybe [:vector
                                  [:vector
                                   [:fn (fn [btn] (instance? b/KeyboardButton btn))]]]]
                         spec.mdl/User
                         [:or :map :nil]]
                        [:map
                         [:inline_keyboard [:vector
                                            [:vector
                                             spec.tg/Button]]]]])


(defn prepare-keyboard
  [kbd user optm]
  (when kbd
    {:inline_keyboard
     (cond-> (mapv (fn [btns] (mapv #(b/to-map % user) btns)) kbd)
       (:temp optm) (conj [(b/to-map (ib/->XButton) user)]))}))


(defn- set-callbacks-message-id
  [user msg]
  (clb/set-new-message-ids
    user
    (:message_id msg)
    (->> msg
         :reply_markup :inline_keyboard flatten
         (map #(some-> % :callback_data java.util.UUID/fromString))
         (filterv some?))))


(defn- to-edit?
  [optm user]
  (when (and (some? (:msg-id user)) (= (:to-edit-msg-id optm) (:msg-id user)))
    (throw-error ::manual-main-message-edit
                 "Manual editing of Main Message is forbidden!" {}))
  (if (:temp optm)
    (some? (:to-edit-msg-id optm))
    (and (some? (:user/msg-id user))
         (not= 0 (:user/msg-id user)))))


(defn- prepare-arguments-map
  [argm kbd optm user]
  (cond-> argm
    true                 (assoc :chat_id (:user/id user))
    (some? kbd)          (assoc :reply_markup kbd)
    (:markdown optm)     (assoc :parse_mode "Markdown")
    (to-edit? optm user) (assoc :message_id (or (:to-edit-msg-id optm) (:user/msg-id user)))))


(defmulti ^:private send-to-chat (fn [& args] (identity (first args))))


(defn- create-media
  [type data]
  {:type (name type)
   :caption (:caption data)
   :media (:file data)})


(defn- prepare-to-multipart
  [args-map]
  (let [files     (atom {})
        args-map# (walk/postwalk
                    (fn [x]
                      (cond (instance? java.io.File x)
                            (let [file-id (keyword (str "fid" (java.util.UUID/randomUUID)))]
                              (swap! files assoc file-id x)
                              (str "attach://" (name file-id)))
                            (number? x) (str x)
                            :else x))
                    (assoc args-map :content-type :multipart))]
    (cond-> args-map#
      (contains? args-map# :media)        (update :media generate-string)
      (contains? args-map# :reply_markup) (update :reply_markup generate-string)
      true (merge @files))))


(defn- send-new-media-to-chat
  [type args-map media]
  (let [media-is-file? (instance? java.io.File (:media media))
        method         (keyword (str "send" (-> type name str/capitalize)))
        args-map#      (cond-> (merge args-map media)
                         true (dissoc :media)
                         true (assoc type (:media media))
                         media-is-file? (prepare-to-multipart))]
    (api-wrap method args-map#)))


(defn- edit-media-in-chat
  [type args-map media]
  (let [media-is-file? (instance? java.io.File (:media media))
        args-map#      (cond-> args-map
                         true (assoc :media media)
                         media-is-file? (prepare-to-multipart))]
    (try (api-wrap :editMessageMedia args-map#)
         (catch clojure.lang.ExceptionInfo ex
           (if (= 400 (-> ex ex-data :status))
             (send-new-media-to-chat type args-map media)
             (throw-error ::edit-message-media-failed
                          "Request to :editMessageMedia failed!"
                          {:args args-map#
                           :error ex}))))))


(defn- send-media-to-chat
  [type user data kbd optm]
  (let [media    (create-media type data)
        args-map (prepare-arguments-map {} kbd optm user)
        new-msg  (if (to-edit? optm user)
                   (edit-media-in-chat type args-map media)
                   (send-new-media-to-chat type args-map media))]
    (set-callbacks-message-id user new-msg)
    new-msg))


(defmethod send-to-chat :photo
  [& args]
  (apply send-media-to-chat args))


(defmethod send-to-chat :document
  [& args]
  (apply send-media-to-chat args))


(defmethod send-to-chat :invoice
  [_ user data kbd optm]
  (let [argm (prepare-arguments-map data kbd optm user)
        new-msg (api-wrap 'send-invoice argm)]
    (set-callbacks-message-id user new-msg)))


(defn- send-message-to-chat
  [argm to-edit??]
  (let [func-kw (if to-edit?? :editMessageText :sendMessage)]
    (try (api-wrap func-kw argm)
         (catch clojure.lang.ExceptionInfo ex
           (if (= 400 (-> ex ex-data :status))
             (api-wrap :sendMessage argm)
             (throw-error ::edit-message-media-failed
                          "Request to :editMessageText failed!"
                          {:args argm
                           :error ex}))))))


(defmethod send-to-chat :message
  [_ user text kbd optm]
  (let [argm       (prepare-arguments-map {:text text :entities []} kbd optm user)
        new-msg    (send-message-to-chat argm (to-edit? optm user))
        new-msg-id (:message_id new-msg)]
    (log/debug ::send-to-chat-message
               "Message sent to chat: %s %s %s %s" optm new-msg-id new-msg user
               {})
    (when (and (not (:temp optm)) (not= new-msg-id (:msg-id user)))
      (u/set-msg-id user new-msg-id))
    (set-callbacks-message-id user new-msg)))


(defn prepare-and-send
  [type user data kbd & opts]
  (let [optm (prepare-options-map opts)
        keyboard (prepare-keyboard kbd user optm)]
    (send-to-chat type user data keyboard optm)))


(defn- download-file
  [file-path]
  (let [uri (format "https://api.telegram.org/file/bot%s/%s"
                    @app/bot-token file-path)
        bis  (-> uri http/get deref :body)
        file (fs/file fs/temp-dir (java.util.UUID/randomUUID))
        fos  (java.io.FileOutputStream. file)]
    (try
      (.transferTo bis fos)
      (finally
        (.close fos)))
    file))


(defn get-file
  [file-id]
  (let [file-path (api-wrap :getFile file-id)]
    (if (fs/exists? file-path)
      (fs/file file-path)
      (download-file file-path))))


(defn delete-message
  [user mid]
  (try
    (api-wrap :deleteMessage {:chat_id (:user/id user)
                              :message_id mid})
    (catch Exception ex
      (log/warn ::delete-message-exception
                "Exception on Message deletion: %s" (ex-message ex)
                {:user user
                 :message-id mid
                 :exception ex})))
  (clb/delete user mid))
