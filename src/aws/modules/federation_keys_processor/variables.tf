variable "lambda_repository_bucket" {
  description = "The name of the bucket which contains the lambda jar"
}

variable "lambda_object_key" {
  description = "The object key of the lambda jar in the s3 bucket"
}

variable "submission_bucket_name" {
  description = "The name of the bucket to store diagnosis keys"
}

variable "interop_base_url" {
  description = "The url of the interop server for exchange of keys"
}

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}