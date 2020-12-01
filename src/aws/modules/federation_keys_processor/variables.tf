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

variable "interop_download_enabled_workspaces" {
  description = "Target environments with enabled download of exposure keys from interop server (allowed values: te-<env>, *, branch)"
  type        = list(string)
}

variable "interop_upload_enabled_workspaces" {
  description = "Target environments with enabled upload of exposure keys to interop server (allowed values: te-<env>, *, branch)"
  type        = list(string)
}

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}