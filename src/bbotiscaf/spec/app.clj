(ns bbotiscaf.spec.app)

(def Bbotiscaf-Config
  [:map
   {:closed true}
   [:api/fn :symbol]
   [:db/conn :string]
   [:bot/default-language-code :keyword]])

(def Project-Config
  [:map
   {:closed true}
   [:bot/token [:re #"^\d{10}:[a-zA-Z0-9_-]{35}$"]]
   [:bot/default-language-code {:optional true} :keyword]
   [:handler/namespaces [:vector :symbol]]
   [:handler/main :symbol]])

(def Config
  [:map
   {:closed true}
   [:api/fn :symbol]
   [:db/conn :string]
   [:bot/token [:re #"^\d{10}:[a-zA-Z0-9_-]{35}$"]]
   [:bot/default-language-code :keyword]
   [:handler/namespaces [:vector :symbol]]
   [:handler/main :symbol]])
