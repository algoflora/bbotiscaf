(ns actions)

(defn greet
  [{:keys [name]}]
  (printf "Hello, %s!\n" name))
