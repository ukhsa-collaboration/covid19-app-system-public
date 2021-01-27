variable "test_order_website" {
  description = "The URL for the website used for virology test orders"
}

variable "test_register_website" {
  description = "The URL for the website to register test results"
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

variable "rate_limit" {
  description = "The requests per second limit for this API gateway"
}

variable "burst_limit" {
  description = "The requests per second burst limit for this API gateway"
}

variable "lambda_repository_bucket" {
  description = "The name of the bucket which contains the lambda jar"
}

variable "lambda_object_key" {
  description = "The object key of the lambda jar in the s3 bucket"
}

variable "test_orders_index" {
  description = "The global secondary index within the test orders table"
}

variable "custom_oai" {
  description = "Secret shared between CloudFront Distribution and Lambda"
}

variable "log_retention_in_days" {
  description = "Days for which events in the associated CloudWatch log group are retained. 0 (the default) means forever"
  type        = number
  default     = 0
}

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "virology_v2_apis_enabled" {
  description = "Feature flag for enabling V2 Virology APIs"
  default     = false
  type        = bool
}
