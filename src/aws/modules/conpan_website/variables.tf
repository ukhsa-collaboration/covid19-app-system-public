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
