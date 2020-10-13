variable "name" {
  description = "Name of the CloudFront distribution"
}

variable "analytics_submission_endpoint" {
  description = "The endpoint for the analytics submission API"
}

variable "analytics_submission_path" {
  description = "The route for the analytics submission API"
}

variable "diagnosis_keys_submission_endpoint" {
  description = "The endpoint for the diagnosis keys submission API"
}

variable "diagnosis_keys_submission_path" {
  description = "The route for the diagnosis keys submission API"
}

variable "exposure_notification_circuit_breaker_endpoint" {
  description = "The endpoint for the exposure notification circuit breaker"
}

variable "exposure_notification_circuit_breaker_path" {
  description = "The route for the exposure notification circuit breaker"
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
