(ns bbotiscaf.button)

(defprotocol KeyboardButton
  (to-map [this user]))

(defrecord TextButton [text func args])

(defn text-btn
  ([text func] (text-btn text func {}))
  ([text func args]
   (->TextButton text func args)))

(defrecord TxtButton [txt func args])

(defn txt-btn
  ([txt func] (txt-btn txt func {}))
  ([txt func args]
   (->TxtButton txt func args)))

(defrecord TxtiButton [txt lang func args])

(defn txti-btn
  [txt lang func args]
  (->TxtiButton txt lang func args))

(defrecord HomeButton [text])

(defn home-btn [text]
  (->HomeButton text))

(defrecord PayButton [text])

(defn pay-btn [text]
  (->PayButton text))

(defrecord UrlButton [text url])

(defn url-btn [text url]
  (->UrlButton text url))

