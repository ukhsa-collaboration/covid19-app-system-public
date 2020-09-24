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