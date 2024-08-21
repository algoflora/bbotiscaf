(ns bbotiscaf.spec.app)

(def bbotiscaf-config-schema
  [:map
   {:closed true}
   [:api/fn :symbol]
   [:db/conn :string]])

(def project-config-schema
  [:map
   {:closed true}
   [:bot/token [:re #"^\d{10}:[a-zA-Z0-9_-]{35}$"]]])

(def config-schema
  [:map
   {:closed true}
   [:api/fn :symbol]
   [:db/conn :string]
   [:bot/token [:re #"^\d{10}:[a-zA-Z0-9_-]{35}$"]]])
