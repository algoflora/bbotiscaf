(ns ^:no-doc bbotiscaf.impl.api
  (:require [bbotiscaf.impl.callback :as clb]
            [bbotiscaf.impl.user :as u]
            [bbotiscaf.impl.system.app :as app]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [babashka.fs :as fs]
            [cheshire.core :refer [generate-string]]
            [babashka.http-client :as http]
            [taoensso.timbre :as log]))

(defn api-wrap
  [method data]
  (let [api-fn @app/api-fn]
    (log/info ::calling-api-fn
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

(defmulti ^:private create-key (fn [kdata _] (type kdata)))

(defmethod create-key clojure.lang.PersistentVector
  [kvec user]
  (create-key {:text (first kvec)
               :func (second kvec)
               :args (nth kvec 2)}
              user))

(defmethod create-key clojure.lang.PersistentArrayMap
  [kmap user]
  (cond-> kmap
    (every? #(contains? kmap %) [:func :args])
    (assoc :callback_data (str (clb/set-callback user (:func kmap) (:args kmap))))

    true (dissoc :func)
    true (dissoc :args)))

(defn- prepare-keyboard
  [kbd user optm]
  (when kbd
    (let [mapf #(cond-> % (and (vector? %) (= 2 (count %))) (conj {}) true (create-key user))]
      {:inline_keyboard
       (cond->> kbd
         true (mapv #(mapv mapf %))
         (:temp optm) (#(conj % [{:text "✖️"
                                  :callback_data (str (clb/set-callback user 'bbotiscaf.handler/delete-this-message {} true))}])))})))

(defn- set-callbacks-message-id
  [user msg]
  (clb/set-new-message-ids
   user
   (:message_id msg)
   (->> msg
        :reply_markup :inline_keyboard flatten
        (mapv #(some-> % :callback_data java.util.UUID/fromString)) (filterv some?))))

(defn- to-edit?
  [optm user]
  (when (and (some? (:msg-id user)) (= (:to-edit-msg-id optm) (:msg-id user)))
    (throw (ex-info "Prohibited way to edit Main Message!" {})))
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
         (catch clojure.lang.ExceptionInfo _  ; TODO: Check for exact Exception
           (send-new-media-to-chat type args-map media)))))

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
    (set-callbacks-message-id user new-msg)
    new-msg))

(defn- send-message-to-chat
  [argm to-edit??]
  (let [func-kw (if to-edit?? :editMessageText :sendMessage)]
    (try (api-wrap func-kw argm)
         (catch clojure.lang.ExceptionInfo _  ; TODO: Check for exact Exception
           (api-wrap :sendMessage argm)))))

(defmethod send-to-chat :message
  [_ user text kbd optm]
  (let [argm       (prepare-arguments-map {:text text} kbd optm user)
        new-msg    (send-message-to-chat argm (to-edit? optm user))
        new-msg-id (:message_id new-msg)]
    (when (and (not (:temp optm)) (not= new-msg-id (:msg-id user)))
      (u/set-msg-id user new-msg-id))
    (set-callbacks-message-id user new-msg)
    new-msg))

(defn prepare-and-send
  [type user data kbd & opts]
  (let [optm (prepare-options-map opts)
        keyboard (prepare-keyboard kbd user optm)]
    (send-to-chat type user data keyboard optm)))

(defn- download-file
  [file-path]
  (let [uri (format "https://api.telegram.org/file/bot%s/%s"
                    (:bot/token @app/bot-token) file-path)
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
  (api-wrap :deleteMessage {:chat_id (:user/id user)
                            :message_id mid})
  (clb/delete user mid))
