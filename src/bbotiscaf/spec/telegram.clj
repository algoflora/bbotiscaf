(ns bbotiscaf.spec.telegram
  (:require [malli.core :as m]
            [malli.util :as mu]))

(def registry (merge (m/default-schemas) (mu/schemas)))

(def user-schema
  [:map
   [:id :int]
   [:is_bot :boolean]
   [:first_name :string]
   [:last_name {:optional true} :string]
   [:username {:optional true} :string]
   [:language_code {:optional true} :string]
   [:is_premium {:optional true} [:= true]]])

(def chat-schema
  [:map
   [:id int?]
   [:type [:enum "private" "group" "supergroup" "channel"]]
   [:title {:optional true} :string]
   [:username {:optional true} :string]
   [:first_name {:optional true} :string]
   [:last_name {:optional true} :string]
   [:is_forum {:optional true} [:= true]]])

(def message-entity-schema
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
   [:user {:optional true} user-schema]
   [:language {:optional true} :string]
   [:custom_emoji_id {:optional true} :string]])

(def Text
  [:map
   {:closed true}
   [:text :string]
   [:entities [:vector message-entity-schema]]])

(def Base-Message
  [:map
   [:message_id :int]
   [:from user-schema]
   [:chat chat-schema]
   [:date :int]])

(def Text-Message
  (m/schema
   [:merge
    Base-Message
    Text]
   {:registry registry}))

(def Message
  (m/schema
   [:merge
    Base-Message
    [:or
     Text]]
   {:registry registry}))

(def Callback-Query
  [:map
   [:id :string]
   [:from user-schema]
   [:message [:or
              Base-Message
              [:map
               [:from chat-schema]
               [:message_id :int]
               [:date [:= 0]]]]]
   [:data [:string {:max 64}]]])

(def Base-Update
  [:map
   [:update_id :int]])

(def message-update-schema
  (m/schema
   [:merge
    Base-Update
    [:map
     [:message Base-Message]]]))

(def Update
  (m/schema
   [:merge
    Base-Update
    [:or ;TODO: find XOR schema
     [:map
      [:message Base-Message]]
     [:map
      [:callback_query Callback-Query]]]]
   {:registry registry}))
