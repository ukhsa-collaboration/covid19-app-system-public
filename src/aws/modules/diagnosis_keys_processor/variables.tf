variable "submission_bucket_name" {
}

variable "distribution_bucket_name" {
}

variable "distribution_id" {
  description = "The CloudFront distribution ID used for cache invalidation"
}

variable "distribution_pattern_daily" {
}

variable "distribution_pattern_2hourly" {
}

variable "mobile_app_bundle" {
  description = " The app bundle ID used to register the mobile apps that correspond to the target environments in this account"
}

variable "lambda_repository_bucket" {
  description = "The name of the bucket which contains the lambda jar"
}

variable "lambda_object_key" {
  description = "The object key of the lambda jar in the s3 bucket"
}

variable "log_retention_in_days" {
  description = "Days for which events in the associated CloudWatch log group are retained. 0 (the default) means forever"
  type        = number
  default     = 0
}

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}

variable "diagnosis_key_submission_prefixes" {
  description = "Allowed prefixes for the diagnosis key submissions"
}

variable "zip_submission_period_offset" {
  description = "The distribution window period offset"
  type        = string
  default     = "PT-15M"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}
