(ns bbotiscaf.config
  (:require
    [bbotiscaf.impl.config :as conf]
    [bbotiscaf.impl.system.app :as app]))


(def profile
  conf/profile)


(defn config
  [& args]
  (reduce (fn [acc key]
            (if (contains? acc key)
              (key acc)
              (throw (ex-info (format "No path %s in project config!" args)
                              {:event ::project-config-path-error
                               :path args
                               :project-config (app/project-config)}))))
          (app/project-config) args))
