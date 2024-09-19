(ns bbotiscaf.impl.button
  (:require
    [bbotiscaf.button :as b]
    [bbotiscaf.impl.callback :as clb]
    [bbotiscaf.impl.system.app :as app]
    [bbotiscaf.texts :refer [txt txti]]))


(defrecord XButton
  [])


(extend-protocol b/KeyboardButton
  b/TextButton
  (to-map [this user]
    {:text (:text this)
     :callback_data
     (str (clb/set-callback user (:func this) (:args this)))})

  b/TxtButton
  (to-map [this user]
    {:text (txt (:txt this))
     :callback_data
     (str (clb/set-callback user (:func this) (:args this)))})

  b/TxtiButton
  (to-map [this user]
    {:text (txti (:txt this) (:lang this))
     :callback_data
     (str (clb/set-callback user (:func this) (:args this)))})

  b/HomeButton
  (to-map [this user]
    (let [text (cond
		 (nil? (:text this))    (txt [:home])
		 (vector? (:text this)) (txt (:text this))
		 :else                  (:text this))]
      {:text text
       :callback_data
       (str (clb/set-callback user (symbol (app/handler-main)) {}))}))

  b/PayButton
  (to-map [this _]
    {:text (:text this)
     :pay true})

  b/UrlButton
  (to-map [this _]
    {:text (:text this)
     :url (:url this)})

  XButton
  (to-map [_ user]
    {:text "✖️"
     :callback_data
     (str (clb/set-callback user 'bbotiscaf.handler/delete-this-message {} true))}))
