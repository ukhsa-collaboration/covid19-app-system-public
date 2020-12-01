variable "name" {
  description = "The name of the distribution module. This should correspond to the API contract"
  type        = string
}

variable "service" {
  description = "The name of the service to provision. This should be distribution or submission"
  type        = string
}

variable "origin_access_identity_path" {
  description = "ARN from the Origin Access Identity"
  type        = string
}

#variable "origin_access_identity" {
#  description = "Origin Access Identity"
#}

variable "logs_bucket_id" {
  description = "The name of the bucket to which S3 access logs are saved."
}

variable "force_destroy_s3_buckets" {
  description = "Force destroy s3 buckets if set to true"
  type        = bool
  default     = true
}

variable "s3_versioning" {
  description = "Enable S3 bucket versioning if set to true"
  type        = bool
  default     = false
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}