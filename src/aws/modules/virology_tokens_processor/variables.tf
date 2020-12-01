variable "lambda_repository_bucket" {
  description = "The name of the bucket which contains the lambda jar"
}

variable "lambda_object_key" {
  description = "The object key of the lambda jar in the s3 bucket"
}

variable "virology_tokens_bucket_name" {
  default = "The S3 bucket where the generated tokens will be stored"
}

variable "test_orders_table_id" {
  description = "The DynamoDB table to persist an incoming test order in"
}

variable "test_results_table_id" {
  description = "The DynamoDB table that contains the test results"
}

variable "virology_submission_tokens_table_id" {
  description = "The DynamoDB table that contains the virology submissions diagnosis keys tokens"
}

variable "test_orders_index" {
  description = "The global secondary index within the test orders table"
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which S3 access logs are saved."
}

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}