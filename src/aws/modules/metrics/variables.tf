variable "env" {
  description = "environment reference"
}

variable "monitored_buckets" {
  type = list(string)
}

variable "cloudfront_upload_id" {}

variable "cloudfront_distribution_id" {}

variable "cloudfront_submission_id" {}

variable "request_triggered" {
  type = list(string)
}

variable "gateways" {
  type = list(string)
}

variable "activation_keys_submission_function" {
  description = "name of lambda function"
}

variable "analytics_submission_function" {
  description = "name of lambda function"
}

variable "analytics_processing_function" {
  description = "name of lambda function"
}

variable "diagnosis_keys_submission_function" {
  description = "name of lambda function"
}

variable "exposure_notification_circuit_breaker_function" {
  description = "name of lambda function"
}

variable "diagnosis_keys_processing_function" {
  description = "name of lambda function"
}

variable "federation_keys_processing_function" {
  description = "name of lambda function"
}

variable "risky_post_districts_upload_function" {
  description = "name of lambda function"
}

variable "risky_venues_circuit_breaker_function" {
  description = "name of lambda function"
}

variable "risky_venues_upload_function" {
  description = "name of lambda function"
}

variable "virology_submission_function" {
  description = "name of lambda function"
}

variable "virology_upload_function" {
  description = "name of lambda function"
}

variable "advanced_analytics_function" {
  description = "name of lambda function"
}

variable "virology_submission_api_gateway_id" {
  description = "id of virology submission api gateway"
}

variable "virology_upload_api_gateway_id" {
  description = "id of virology upload api gateway"
}

