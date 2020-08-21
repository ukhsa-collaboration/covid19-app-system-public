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
