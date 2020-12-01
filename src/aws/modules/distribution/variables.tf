variable "name" {
  description = "The name of the S3 bucket. This should correspond to the API contract"
}

variable "default_payload" {
  description = "The S3 object to serve by default"
}

variable "payload_source" {
  description = "The content to distribute"
  default     = ""
}

variable "metadata_signature" {
  description = "The metadata signature header value"
  default     = ""
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which all S3 access logs are saved."
}

variable "metadata_signature_date" {
  description = "The metadata signature date header value"
  default     = ""
}

variable "force_destroy_s3_buckets" {
  description = "Force destroy s3 buckets if set to true"
}

variable "s3_versioning" {
  description = "Enable S3 bucket versioning if set to true"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}
