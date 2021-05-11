variable "name" {
  description = "Name of the CloudFront distribution"
}

variable "analytics_submission_endpoint" {
  description = "The endpoint for the analytics submission API"
}

variable "analytics_submission_path" {
  description = "The route for the analytics submission API"
}

variable "analytics_events_submission_endpoint" {
  description = "The endpoint for the analytics events submission API"
}

variable "analytics_events_submission_path" {
  description = "The route for the analytics events submission API"
}

variable "diagnosis_keys_submission_endpoint" {
  description = "The endpoint for the diagnosis keys submission API"
}

variable "diagnosis_keys_submission_path" {
  description = "The route for the diagnosis keys submission API"
}

variable "crash_reports_submission_endpoint" {
  description = "The endpoint for the crash reports submission API"
}

variable "crash_reports_submission_path" {
  description = "The route for the crash reports submission API"
}

variable "exposure_notification_circuit_breaker_endpoint" {
  description = "The endpoint for the exposure notification circuit breaker"
}

variable "exposure_notification_circuit_breaker_path" {
  description = "The route for the exposure notification circuit breaker"
}

variable "isolation_payment_endpoint" {
  description = "The endpoint for the isolation payment submission API"
}

variable "isolation_payment_path" {
  description = "The route for the isolation payment submission"
}

variable "risky_venues_circuit_breaker_endpoint" {
  description = "The endpoint for the risky venues circuit breaker"
}

variable "risky_venues_circuit_breaker_path" {
  description = "The route for the risky venues circuit breaker"
}

variable "virology_kit_endpoint" {
  description = "The endpoint for the virology test kit order and result API"
}

variable "virology_kit_path" {
  description = "The route for the virology test kit order and result"
}

variable "domain" {
  description = "The domain the CloudFront distribution needs to be deployed into"
}

variable "web_acl_arn" {
  description = "The ARN of the WAFv2 web acl to filter CloudFront requests"
}

variable "custom_oai" {
  description = "Secret shared between CloudFront Distribution and Lambda"
}

variable "enable_shield_protection" {
  description = "Flag to enable/disable shield protection"
  type        = bool
}

variable "analytics_submission_health_path" {
  description = "The route for the analytics submission health endpoint"
}

variable "diagnosis_keys_submission_health_path" {
  description = "The route for the diagnosis keys submission health endpoint"
}

variable "empty_submission_endpoint" {
  description = "The endpoint for the empty submission api"
}

variable "empty_submission_path" {
  description = "The route for the empty submission"
}

variable "empty_submission_v2_bucket_regional_domain_name" {
  description = "The S3 bucket regional domain name"
}

variable "empty_submission_v2_path" {
  description = "The route for the empty submission V2"
}

variable "empty_submission_v2_origin_access_identity_path" {
  description = "The origin access controlling access to the store"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "isolation_payment_health_path" {
  description = "The route for the isolation payment health endpoint"
}

variable "analytics_events_submission_health_path" {
  description = "The route for the analytics events submission health endpoint"
}
