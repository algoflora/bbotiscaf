(ns bbotiscaf.spec.model)

(def User
  [:map
   {:closed true}
   [:db/id :int]
   [:user/uuid :uuid]
   [:user/username {:optional true} :string]
   [:user/id :int]
   [:user/furst-name :string]
   [:user/last-name {:optional true} :string]
   [:user/language-code {:optional true} :string]
   [:user/msg-id {:optional true} :int]])

(def Callback
  [:map
   [:db/id :int]
   [:callback/uuid :uuid]
   [:callback/function :symbol]
   [:callback/arguments :map]
   [:callback/user [:or [:map [:db/id :int]] User]]
   [:callback/is-service :boolean]
   [:callback/message-id {:optional true} :int]])
