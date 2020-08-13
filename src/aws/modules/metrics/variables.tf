variable "env" {
  description = "environment reference"
}

variable "monitored_buckets" {
  type = list(string)
}

variable "cloudfront_upload_id" {}

variable "cloudfront_distribution_id" {}

variable "cloudfront_submission_id" {}

variable "submission_lambdas" {
  type = list(string)
}

variable "upload_lambdas" {
  type = list(string)
}

variable "processing_lambdas" {
  type = list(string)
}

variable "circuit_breaker_lambdas" {
  type = list(string)
}

variable "request_triggered" {
  type = list(string)
}

variable "gateways" {
  type = list(string)
}