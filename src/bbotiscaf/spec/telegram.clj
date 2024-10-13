(ns bbotiscaf.spec.telegram
  (:require
    [malli.core :as m]
    [malli.util :as mu]))


(def registry (merge (m/default-schemas) (mu/schemas)))


(def User
  [:map
   {:closed true}
   [:id :int]
   [:is_bot :boolean]
   [:first_name :string]
   [:last_name {:optional true} :string]
   [:username {:optional true} :string]
   [:language_code {:optional true} :string]
   [:is_premium {:optional true} [:= true]]])


(def Chat
  [:map
   {:closed true}
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
           "mention" "hashtag" "cashtag" "bot_command" "url"
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
   [:entities {:optional true} [:maybe [:vector MessageEntity]]]])


(def Button
  [:map
   {:closed true}
   [:text :string]
   [:url {:optional true} [:re #"^https?://(?:www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b(?:[-a-zA-Z0-9()@:%_\+.~#?&//=]*)$"]]
   [:callback_data {:optional true} [:string {:min 1 :max 64}]]
   [:pay {:optional true} [:= true]]])


(def ReplyMarkup
  [:map
   {:closed true}
   [:reply_markup {:optional true} [:map
                                    [:inline_keyboard [:vector
                                                       [:vector
                                                        Button]]]]]])


(def BaseMessage
  (m/schema
    [:merge
     [:map
      {:closed true}
      [:message_id {:optional true} :int]
      [:from  User]
      [:chat Chat]
      [:date :int]
      [:edit_date {:optional true} :int]]
     ReplyMarkup]
    {:registry registry}))


(def TextMessage
  (m/schema
    [:merge
     BaseMessage
     Text]
    {:registry registry}))


(def Invoice
  [:map
   {:closed true}
   [:title [:string {:min 1 :max 32}]]
   [:description [:string {:min  1 :max 255}]]
   [:payload [:string {:min 1 :max 128}]]
   [:provider_token :string]
   [:currency [:string {:min 3 :max 3}]]
   [:prices :string]])


(def InvoiceMessage
  (m/schema
    [:merge
     BaseMessage
     Invoice]
    {:registry registry}))


(def Message
  [:or
   TextMessage
   InvoiceMessage])


(def BaseCallbackQuery
  [:map
   {:closed true}
   [:id :string]
   [:from User]])


(def CallbackQuery
  (m/schema
    [:merge
     BaseCallbackQuery
     [:map
      {:closed true}
      [:message {:optional true} [:or
                                  Message
                                  [:map
                                   [:from User]
                                   [:message_id :int]
                                   [:date [:= 0]]]]]
      [:data {:optional true} [:string {:min 1 :max 64}]]]]
    {:registry registry}))


(def PreCheckoutQuery
  [:map
   {:closed true}
   [:id :string]
   [:from User]
   [:currency [:string {:min 3 :max 3}]]
   [:total_amount [:int {:min 0}]]
   [:invoice_payload [:string {:min 1 :max 128}]]])


(def PreCheckoutQueryUpdateData
  [:map
   {:closed true}
   [:pre_checkout_query PreCheckoutQuery]])


(def MessageUpdateData
  [:map
   {:closed true}
   [:message Message]])


(def CallbackQueryUpdateData
  [:map
   {:closed true}
   [:callback_query CallbackQuery]])


(def UpdateData
  [:or
   MessageUpdateData
   CallbackQueryUpdateData
   PreCheckoutQueryUpdateData])


(def BaseUpdate
  [:map
   {:closed true}
   [:update_id :int]])


(def MessageUpdate
  (m/schema
    [:merge
     BaseUpdate
     MessageUpdateData]
    {:registry registry}))


(def CallbackQueryUpdate
  (m/schema
    [:merge
     BaseUpdate
     CallbackQueryUpdateData]
    {:registry registry}))


(def PreCheckoutQueryUpdate
  (m/schema
    [:merge
     BaseUpdate
     PreCheckoutQueryUpdateData]
    {:registry registry}))


(def Update
  [:or
   MessageUpdate
   CallbackQueryUpdate
   PreCheckoutQueryUpdate])


(def BaseRequest
  [:map
   {:closed true}
   [:chat_id :int]])


(def SendMessageRequest
  (m/schema
    [:merge
     BaseRequest
     [:map
      {:closed true}
      [:text :string]
      [:entities [:maybe [:vector MessageEntity]]]]
     ReplyMarkup]
    {:registry registry}))


(def EditMessageTextRequest
  (m/schema
    [:merge
     SendMessageRequest
     [:map
      {:closed true}
      [:message_id :int]]]
    {:registry registry}))


(def DeleteMessageRequest
  (m/schema
    [:merge
     BaseRequest
     [:map
      {:closed true}
      [:message_id :int]]]
    {:registry registry}))


(def Request
  [:or
   SendMessageRequest
   EditMessageTextRequest
   DeleteMessageRequest])
