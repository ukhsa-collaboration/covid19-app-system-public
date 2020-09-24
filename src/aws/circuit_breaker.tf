module "exposure_notification_circuit_breaker" {
  source                   = "./modules/circuit_breaker"
  name                     = "exposure-notification-circuit-breaker"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_handler_class     = "uk.nhs.nhsx.circuitbreakers.ExposureNotificationHandler"
  burst_limit              = var.burst_limit
  rate_limit               = var.rate_limit
  ssm_parameter            = "exposure-notification"
  custom_oai               = random_uuid.submission-custom-oai.result
  alarm_topic_arn          = var.alarm_topic_arn
}

module "risky_venues_circuit_breaker" {
  source                   = "./modules/circuit_breaker"
  name                     = "risky-venues-circuit-breaker"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_handler_class     = "uk.nhs.nhsx.circuitbreakers.RiskyVenueHandler"
  burst_limit              = var.burst_limit
  rate_limit               = var.rate_limit
  ssm_parameter            = "venue-notification"
  custom_oai               = random_uuid.submission-custom-oai.result
  alarm_topic_arn          = var.alarm_topic_arn
}

output "exposure_notification_circuit_breaker_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/circuit-breaker/exposure-notification"
}
output "risky_venues_circuit_breaker_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/circuit-breaker/venue"
}
# Health endpoints
output "exposure_notification_circuit_breaker_health_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/circuit-breaker/exposure-notification/health"
}
output "risky_venues_circuit_breaker_health_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/circuit-breaker/venue/health"
}
