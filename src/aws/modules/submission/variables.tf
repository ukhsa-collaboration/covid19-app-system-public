variable "name" {
  description = "The name of the submission module. This should correspond to the API contract"
}

variable "lambda_handler_class" {
  description = "The full classname for the handler function"
}

variable "lambda_environment_variables" {
  description = "A map of environment variable --> value to pass as environment to the lambda"
  type        = map
}

variable "rate_limit" {
  description = "The requests per second limit for this API gateway"
}

variable "burst_limit" {
  description = "The requests per second burst limit for this API gateway"
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which all S3 access logs are saved."
}

variable "lambda_repository_bucket" {
  description = "The name of the bucket which contains the lambda jar"
}

variable "lambda_object_key" {
  description = "The object key of the lambda jar in the s3 bucket"
}

variable "force_destroy_s3_buckets" {
  description = "Force destroy s3 buckets if set to true"
}

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}

variable "replication_enabled" {
  description = "will enable bucket versioning and backup bucket contents in secondary bucket"
  type        = bool
  default     = false
}

variable "provisioned_concurrent_executions" {
  description = "provisioned concurrency or 0 (no provisioned concurrency)"
  type        = number
  default     = 0
}
