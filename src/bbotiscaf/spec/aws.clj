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
   [:md5OfBody :string]
   [:eventSourceARN :string]
   [:awsRegion :string]
   [:messageId :string]
   [:eventSource :string]
   [:messageAttributes [:map]]
   [:body :string]
   [:receiptHandle :string]
   [:attributes
    [:map
     {:closed true}
     [:ApproximateReceiveCount :string]
     [:SentTimestamp :string]
     [:SequenceNumber :string]
     [:MessageGroupId :string]
     [:SenderId :string]
     [:MessageDeduplicationId :string]
     [:ApproximateFirstReceiveTimestamp :string]
     [:AWSTraceHeader :string]]]])


(def SQSRecordsBunch
  [:map
   [:Records [:vector SQSRecordSchema]]])
