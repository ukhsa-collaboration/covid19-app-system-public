module "cloudwatch_analytics" {
  source                     = "./modules/metrics"
  env                        = terraform.workspace
  cloudfront_distribution_id = module.distribution_apis.distribution_id
  cloudfront_submission_id   = module.submission_apis.distribution_id
  cloudfront_upload_id       = module.upload_apis.distribution_id
  monitored_buckets = [
    module.diagnosis_keys_distribution_store.bucket_id,
    module.analytics_submission.store_id,
    module.diagnosis_keys_submission.store_id,
    module.analytics_processing.output_store_id
  ]
  submission_lambdas = [
    module.analytics_submission.function,
    module.diagnosis_keys_submission.function,
    module.activation_keys_submission.function,
    module.virology_submission.lambda_function_name
  ]
  upload_lambdas = [
    module.risky_post_districts_upload.lambda_function_name,
    module.risky_venues_upload.lambda_function_name,
    module.virology_upload.lambda_function_name
  ]
  processing_lambdas = [
    module.analytics_processing.function,
    module.diagnosis_keys_processing.function,
    module.advanced_analytics.lambda_function_name
  ]
  circuit_breaker_lambdas = [
    module.exposure_notification_circuit_breaker.function,
    module.risky_venues_circuit_breaker.function
  ]
  request_triggered = [
    module.risky_venues_upload.lambda_function_name,
    module.risky_post_districts_upload.lambda_function_name,
    module.virology_submission.lambda_function_name,
    module.diagnosis_keys_submission.function,
    module.risky_venues_circuit_breaker.function,
    module.exposure_notification_circuit_breaker.function,
    module.analytics_submission.function,
    module.virology_upload.lambda_function_name,
    module.activation_keys_submission.function
  ]
  gateways = [
    module.activation_keys_submission.gateway_id,
    module.diagnosis_keys_submission.gateway_id,
    module.risky_post_districts_upload.gateway_id,
    module.virology_upload.gateway_id,
    module.analytics_submission.gateway_id,
    module.risky_venues_upload.gateway_id,
    module.virology_submission.gateway_id,
    module.risky_venues_circuit_breaker.gateway_id,
    module.exposure_notification_circuit_breaker.gateway_id
  ]
}