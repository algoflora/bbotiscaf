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

(def MessageEntity
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
   [:entities [:vector MessageEntity]]])

(def Button
  [:map
   {:closed true}
   [:text :string]
   [:url {:optional true} [:re #"^https?://(?:www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b(?:[-a-zA-Z0-9()@:%_\+.~#?&//=]*)$"]]
   [:callback_data {:optional true} [:string {:min 1 :max 64}]]
   [:pay {:optional true} [:= true]]])

(def Reply-Markup
  [:map
   [:reply_markup {:optional true} [:map
                                    [:inline_keyboard [:vector
                                                       [:vector
                                                        Button]]]]]])

(def BaseMessage
  (m/schema
   [:merge
    [:map
     [:message_id {:optional true} :int]
     [:from  User]
     [:chat Chat]
     [:date :int]]
    Reply-Markup]
   {:registry registry}))

(def TextMessage
  (m/schema
   [:merge
    BaseMessage
    Text]
   {:registry registry}))

(def Message
  [:or
   TextMessage])

(def BaseCallbackQuery
  [:map
   [:id :string]
   [:from User]])

(def CallbackQuery
  (m/schema
   [:merge
    BaseCallbackQuery
    [:map
     [:message {:optional true} [:or
                                 Message
                                 [:map
                                  [:from User]
                                  [:message_id :int]
                                  [:date [:= 0]]]]]
     [:data {:optional true} [:string {:min 1 :max 64}]]]]
   {:registry registry}))

(def MessageUpdateData
  [:map
   [:message Message]])

(def CallbackQueryUpdateData
  [:map
   [:callback_query CallbackQuery]])

(def Update-Data
  [:or
   MessageUpdateData
   CallbackQueryUpdateData])

(def Base-Update
  [:map
   [:update_id :int]])

(def Message-Update
  (m/schema
   [:merge
    Base-Update
    MessageUpdateData]
   {:registry registry}))

(def CallbackQueryUpdate
  (m/schema
   [:merge
    Base-Update
    CallbackQueryUpdateData]
   {:registry registry}))

(def Update
  [:or
   Message-Update
   CallbackQueryUpdate])

(def BaseRequest
  [:map
   [:chat_id :int]])

(def SendMessageRequest
  (m/schema
   [:merge
    BaseRequest
    [:map
     [:text :string]
     [:entities [:vector MessageEntity]]]
    Reply-Markup]
   {:registry registry}))

(def EditMessageTextRequest
  (m/schema
   [:merge
    SendMessageRequest
    [:map
     [:message_id :int]]]
   {:registry registry}))

(def DeleteMessageRequest
  (m/schema
   [:merge
    BaseRequest
    [:map
     [:message_id :int]]]
   {:registry registry}))

(def Request
  [:or
   SendMessageRequest
   DeleteMessageRequest])
