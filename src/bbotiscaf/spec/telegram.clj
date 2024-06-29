(ns bbotiscaf.spec.telegram)

(def user-schema
  [:map
   [:id :int]
   [:is_bot :boolean]
   [:first_name :string]
   [:last_name {:optional true} :string]
   [:username {:optional true} :string]
   [:language_code {:optional true} :string]])

(def chat-schema
  [:map
   [:id int?]
   [:type [:enum "private" "group" "supergroup" "channel"]]
   [:title {:optional true} :string]
   [:username {:optional true} :string]
   [:first_name {:optional true} :string]
   [:last_name {:optional true} :string]])

(def message-schema
  [:map
   [:message_id :int]
   [:from user-schema]
   [:chat chat-schema]
   [:date :int]
   [:text {:optional true} :string]])

(def callback_query-schema
  [:map
   [:id :string]
   [:from user-schema]
   [:message [:or
              message-schema
              [:map
               [:from chat-schema]
               [:message_id :int]
               [:date [:= 0]]]]]
   [:data [:string {:max 64}]]])

(def update-schema
  [:map
   [:update_id int?]
   [:message {:optional true} message-schema]
   [:callback_query {:optional true} callback_query-schema]])
