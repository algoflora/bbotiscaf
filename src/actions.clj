(ns actions)

(defn greet
  [{:keys [name]}]
  (printf "Namaste, %s!\n" name))
