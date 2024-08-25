(ns bbotiscaf.spec.telegram
  (:require [malli.core :as m]
            [malli.util :as mu]))

(def registry (merge (m/default-schemas) (mu/schemas)))

(def User
  [:map
   [:id :int]
   [:is_bot :boolean]
   [:first_name :string]
   [:last_name {:optional true} :string]
   [:username {:optional true} :string]
   [:language_code {:optional true} :string]
   [:is_premium {:optional true} [:= true]]])

(def Chat
  [:map
   [:id :int]
   [:type [:enum "private" "group" "supergroup" "channel"]]
   [:title {:optional true} :string]
   [:username {:optional true} :string]
   [:first_name {:optional true} :string]
   [:last_name {:optional true} :string]
   [:is_forum {:optional true} [:= true]]])

(def Message-Entity
  [:map
   {:closed true}
   [:type [:enum
           "mention" "hashtag" "cashtag" "bot_comand" "url"
           "email" "phone_number" "bold" "italic" "underline"
           "strikethrough" "spoiler" "blockquote" "expandable_blockquote"
           "code" "pre" "text_link" "text_mension" "custom_emoji"]]
   [:offset :int]
   [:length :int]
   [:url {:optional true} :string]
   [:user {:optional true} User]
   [:language {:optional true} :string]
   [:custom_emoji_id {:optional true} :string]])

(def Text
  [:map
   {:closed true}
   [:text :string]
   [:entities [:vector Message-Entity]]])

(def Button
  [:map
   {:closed true}
   [:text :string]
   [:url {:optional true} [:re #"^https?://(?:www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b(?:[-a-zA-Z0-9()@:%_\+.~#?&//=]*)$"]]
   [:callback_data {:optional true} [:string {:min 1 :max 64}]]
   [:pay {:optional true} [:= true]]])

(def Base-Message
  [:map
   [:message_id {:optional true} :int]
   [:from  User]
   [:chat Chat]
   [:date :int]
   [:reply_markup {:optional true} [:map
                                    [:inline_keyboard [:vector
                                                       [:vector
                                                        Button]]]]]])

(def Text-Message
  (m/schema
   [:merge
    Base-Message
    Text]
   {:registry registry}))

(def Message
  [:or
   Text-Message])

(def Callback-Query
  [:map
   [:id :string]
   [:from User]
   [:message [:or
              Message
              [:map
               [:from Chat]
               [:message_id :int]
               [:date [:= 0]]]]]
   [:data [:string {:min 1 :max 64}]]])

(def Message-Update-Data
  [:map
   [:message Message]])

(def Callback-Query-Update-Data
  [:map
   [:callback_query Callback-Query]])

(def Update-Data
  [:or
   Message-Update-Data
   Callback-Query-Update-Data])

(def Base-Update
  [:map
   [:update_id :int]])

(def Message-Update
  (m/schema
   [:merge
    Base-Update
    Message-Update-Data]
   {:registry registry}))

(def Callback-Query-Update
  (m/schema
   [:merge
    Base-Update
    Callback-Query-Update-Data]
   {:registry registry}))

(def Update
  [:or
   Message-Update
   Callback-Query-Update])

(def Base-Request
  [:map
   [:chat_id :int]])

(def Send-Message-Request
  (m/schema
   [:merge
    Base-Request
    [:map
     [:text :string]
     [:entities [:vector Message-Entity]]]]
   {:registry registry}))

(def Delete-Message-Request
  (m/schema
   [:merge
    Base-Request
    [:map
     [:message_id :int]]]
   {:registry registry}))

(def Request
  [:or
   Send-Message-Request
   Delete-Message-Request])
