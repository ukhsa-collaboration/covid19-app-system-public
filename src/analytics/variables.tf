variable "force_destroy_s3_buckets" {
  description = "Force destroy s3 buckets if set to true"
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which all S3 access logs are saved"
}

variable "risky_post_codes_bucket_id" {
  description = "The name of the bucket the stores risky post codes"
}

variable "analytics_submission_store_parquet_bucket_id" {
  description = "The name of the bucket the stores mobile analytics"
}

variable "analytics_submission_store_consolidated_parquet_bucket_id" {
  description = "The name of the bucket the stores mobile analytics"
}

variable "analytics_submission_events_bucket_id" {
  description = "The name of the bucket the stores mobile event analytics"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "sip_analytics_bucket_location" {
  description = "The SIP Analytics S3 bucket"
  type        = string
}

variable "circuit_breaker_stats_bucket_id" {
  description = "The Circuit Breaker Analytics S3 bucket"
}

variable "key_federation_download_stats_bucket_id" {
  description = "The key federation analytics download S3 bucket"
}

variable "diagnosis_key_submission_stats_bucket_id" {
  description = "The diagnosis key submission S3 bucket"
}

variable "key_federation_upload_stats_bucket_id" {
  description = "The key federation analytics upload S3 bucket"
}

variable "log_retention_in_days" {
  description = "Days for which events in the associated CloudWatch log group are retained. 0 (the default) means forever"
  type        = number
  default     = 0
}

variable "query_completion_polling_interval_seconds" {
  description = "How often the system checks that a query has completed"
}

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}

variable "edge_export_url" {
  description = "URL of azure container where the export should go to"
}

variable "edge_export_mobile_analytics_enabled_workspaces" {
  description = "Target environments with enabled SQS processing (allowed values: te-<env>)"
  type        = list(string)
}
variable "aae_export_enabled_workspaces" {
  description = "Target environments with enabled SQS processing (allowed values: te-<env>, branch)"
  type        = list(string)
}

variable "aae_mobile_analytics_url_prefix" {
  description = "HTTPS PUT target"
}

variable "aae_mobile_analytics_url_suffix" {
  description = "HTTPS PUT target (e.g. empty string)"
}

variable "aae_mobile_analytics_p12_cert_secret_name" {
  description = "Name of the SecretsManager secret containing the TLS client cert in (binary secret: .p12 format)"
}

variable "aae_mobile_analytics_p12_cert_password_secret_name" {
  description = "Name of the SecretsManager secret containing the password of the TLS client cert (string secret)"
}

variable "aae_mobile_analytics_subscription_secret_name" {
  description = "Name of the SecretsManager secret containing the Ocp-Apim-Subscription-Key HTTP header value (string secret)"
}
variable "aae_mobile_analytics_events_url_prefix" {
  description = "HTTPS PUT target"
}

variable "aae_mobile_analytics_events_url_suffix" {
  description = "HTTPS PUT target (e.g. ?feedName=Epidemiological)"
}

variable "aae_mobile_analytics_events_p12_cert_secret_name" {
  description = "Name of the SecretsManager secret containing the TLS client cert in (binary secret: .p12 format)"
}

variable "aae_mobile_analytics_events_p12_cert_password_secret_name" {
  description = "Name of the SecretsManager secret containing the password of the TLS client cert (string secret)"
}

variable "aae_mobile_analytics_events_subscription_secret_name" {
  description = "Name of the SecretsManager secret containing the Ocp-Apim-Subscription-Key HTTP header value (string secret)"
}
