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
