variable "base_domain" {
  description = "The base DNS domain for the APIs"
}

variable "mobile_app_bundle" {
  description = " The app bundle ID used to register the mobile apps that correspond to the target environments in this account"
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

variable "waf2_web_acl" {
  description = "The name of the WAFv2 web acl to filter CloudFront requests"
}

variable "aae_environment" {
  description = "The name of the AAE endpoint base domain in DNS (set in main.tf in the appropriate src/aws/accounts subdirectory)"
}

variable "virology_test_order_website" {
  description = "The website to order test kits for virology testing"
}

variable "virology_test_register_website" {
  description = "The website to register for test kits for virology testing"
}
