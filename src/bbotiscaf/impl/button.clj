(ns bbotiscaf.impl.button
  (:require [bbotiscaf.button :as b]
            [bbotiscaf.impl.callback :as clb]
            [bbotiscaf.impl.system.app :as app]
            [bbotiscaf.texts :refer [txt txti]]
            [bbotiscaf.dynamic :refer [*user*]]))

(defrecord XButton [user])

(extend-protocol b/KeyboardButton
  b/TextButton
  (to-map [this]
    {:text (:text this)
     :callback_data
     (str (clb/set-callback *user* (:func this) (:args this)))})

  b/TxtButton
  (to-map [this]
    {:text (txt (:txt this))
     :callback_data
     (str (clb/set-callback *user* (:func this) (:args this)))})

  b/TxtiButton
  (to-map [this]
    {:text (txti (:txt this) (:lang this))
     :callback_data
     (str (clb/set-callback *user* (:func this) (:args this)))})

  b/HomeButton
  (to-map [this]
    {:text (:text this)
     :callback_data
     (str (clb/set-callback *user* (symbol @app/handler-main) {}))})

  b/PayButton
  (to-map [this]
    {:text (:text this)
     :pay true})

  b/UrlButton
  (to-map [this]
    {:text (:text this)
     :url (:url this)})

  XButton
  (to-map [this]
    {:text "✖️"
     :callback_data
     (str (clb/set-callback (:user this) 'bbotiscaf.handler/delete-this-message {} true))}))

