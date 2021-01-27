variable "name" {
  description = "The name of the API"
}

variable "bucket_name" {
  description = "The distribution API store bucket where the upload API persists its data"
}

variable "distribution_id" {
  description = "The CloudFront distribution ID used for cache invalidation"
}

variable "distribution_invalidation_pattern" {
  description = "The path pattern to use for cache invalidation"
}

variable "lambda_handler_class" {
  description = "The full class name for the lambda entry point (handler)"
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

variable "should_parse_additional_fields" {
  description = "Feature flag for risky venue upload with message type"
  default     = false
  type        = bool
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}
