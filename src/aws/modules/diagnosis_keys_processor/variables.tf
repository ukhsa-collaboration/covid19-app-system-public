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

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}

variable "diagnosis_key_submission_prefixes" {
  description = "Allowed prefixes for the diagnosis key submissions"
}