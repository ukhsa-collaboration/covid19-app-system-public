variable "force_destroy_s3_buckets" {
  description = "Force destroy s3 buckets if set to true"
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which all S3 access logs are saved"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "query_completion_polling_interval_seconds" {
  description = "How often the system checks that a query has completed"
}

variable "log_retention_in_days" {
  description = "Days for which events in the associated CloudWatch log group are retained. 0 (the default) means forever"
  type        = number
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
variable "edge_export_url" {
  description = "URL of azure container where the export should go to"
}

variable "enabled_workspaces" {
  description = "Target environments with enabled SQS processing (allowed values: te-<env>)"
  type        = list(string)
}

variable "mobile_analytics_table" {
  description = "name of table to be used for mobile analytics"
}
