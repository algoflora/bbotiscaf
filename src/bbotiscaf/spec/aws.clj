(ns bbotiscaf.spec.aws)


(def SQSContext
  [:map
   {:closed true}
   ["content-length" {:optional true} :string]
   ["content-type" :string]
   ["date" :string]
   ["lambda-runtime-aws-request-id" :string]
   ["lambda-runtime-deadline-ms" :string]
   ["lambda-runtime-invoked-function-arn" :string]
   ["lambda-runtime-trace-id" :string]])


(def SQSRecordSchema
  [:map
   {:closed true}
   [:attributes
    [:map
     {:closed true}
     [:AWSTraceHeader {:optional true} :string]
     [:ApproximateFirstReceiveTimestamp {:optional true} :string]
     [:ApproximateReceiveCount {:optional true} :string]
     [:MessageDeduplicationId {:optional true} :string]
     [:MessageGroupId {:optional true} :string]
     [:SenderId {:optional true} :string]
     [:SentTimestamp {:optional true} :string]
     [:SequenceNumber {:optional true} :string]
     [:SqsManagedSseEnabled {:optional true} :string]]]
   [:awsRegion :string]
   [:body :string]
   [:eventSource :string]
   [:eventSourceARN :string]
   [:md5OfBody :string]
   [:messageAttributes [:map]]
   [:messageId :string]
   [:receiptHandle :string]])


(def SQSRecordsBunch
  [:map
   [:Records [:vector SQSRecordSchema]]])
