variable "name" {
  description = "The name of the store"
}

variable "base_domain" {
  description = "The app system base dns"
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which all S3 access logs are saved."
}

variable "aws_wafv2_web_acl_arn" {
  description = "The ACL arn registered in WAF"
}

variable "force_destroy_s3_buckets" {
  description = "Force destroy s3 buckets if set to true"
}

variable "enable_shield_protection" {
  description = "Flag to enable/disable shield protection"
  type        = bool
}