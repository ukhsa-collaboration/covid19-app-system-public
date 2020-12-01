variable "logs_bucket_id" {
  description = "The name of the bucket to which all S3 access logs are saved."
}

variable "force_destroy_s3_buckets" {
  description = "Force destroy s3 buckets if set to true"
  type        = bool
  default     = true
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}
