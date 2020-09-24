variable "name" {
  description = "Name of the CloudFront distribution"
}

variable "domain" {
  description = "The domain the CloudFront distribution needs to be deployed into"
}

variable "web_acl_arn" {
  description = "The ARN of the WAFv2 web acl to filter CloudFront requests"
}

variable "risky_venues_messages_bucket_regional_domain_name" {
  description = "The S3 bucket regional domain name used to construct the URLs for risky venues data"
}

variable "risky_venues_messages_payload" {
  description = "The path (i.e. route) to the venues payload"
}

variable "risky_venues_messages_origin_access_identity_path" {
  description = "The origin access controlling access to the store"
}
