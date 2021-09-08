variable "base_domain" {
  description = "The base DNS domain for the APIs"
}

variable "mobile_app_bundle" {
  description = "The app bundle ID used to register the mobile apps that correspond to the target environments in this account"
}

variable "rate_limit" {
  description = "The requests per second limit for this API gateway"
}

variable "burst_limit" {
  description = "The requests per second burst limit for this API gateway"
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which all S3 access logs are saved"
}

variable "virology_submission" {
  description = "The config for virology submission"
  type        = map(map(string))
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

variable "log_retention_in_days" {
  description = "Days for which events in the associated CloudWatch log group are retained. 0 (the default) means forever"
  type        = number
  default     = 0
}

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}

variable "force_destroy_s3_buckets" {
  description = "Force destroy s3 buckets if set to true"
}

variable "s3_versioning" {
  description = "Enable S3 bucket versioning if set to true"
}

variable "enable_shield_protection" {
  description = "Flag to enable/disable shield protection"
  type        = bool
}

variable "shield_ddos_alarms_sns_arn" {
  description = "ARN of SNS topic subscribed to shield alarms state transition"
}

variable "waf_arn" {
  description = "ARN of WAF to be attached to CloudFront distributions"
}

variable "submission_replication_enabled" {
  description = "will enable bucket versioning and backup bucket contents in secondary bucket"
  type        = bool
  default     = false
}

variable "analytics_submission_scale_up_provisioned_concurrent_executions" {
  description = "provisioned concurrency or 0 (no scheduled provisioned concurrency)"
  type        = number
}

variable "analytics_submission_scale_up_cron" {
  description = "cron schedule"
}

variable "analytics_submission_scale_down_provisioned_concurrent_executions" {
  description = "provisioned concurrency or 0 (no scheduled provisioned concurrency)"
  type        = number
}

variable "analytics_submission_scale_down_cron" {
  description = "cron schedule"
}

variable "analytics_submission" {
  description = "Analytics submission config: ('ingestion_interval'). Keys: target environment or 'default'"
  type        = map(map(string))
}

variable "analytics_queue_processor_reserved_concurrent_executions" {
  description = "reserved concurrency or -1 (no reserved concurrency)"
  type        = number
}

variable "analytics_aws_accounts" {
  description = "list of analytics aws account numbers to enable readonly access to the bucket for athena queries and quicksight"
  type        = list(string)
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "isolation_token_expiry_in_weeks" {
  description = "The time to live for the isolation token in weeks"
  type        = number
  default     = 2
}

variable "isolation_payment" {
  description = "Isolation payment configuraton ('enabled', 'gateway_website_prefix', 'countries_whitelisted'). Keys: target environment or 'default'"
  type        = map(map(string))
}

variable "zip_submission_period_offset" {
  description = "The distribution window period offset"
  type        = map(string)
}

variable "distribution_cache_ttl" {
  description = "Time to live for the cloudfront distribution cache"
}