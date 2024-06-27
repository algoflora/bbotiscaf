(ns events)

(defn greet
  [{:keys [name]}]
  (print (format "Hello, %s!\n" name)))
