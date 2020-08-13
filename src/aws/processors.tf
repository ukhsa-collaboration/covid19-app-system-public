module "analytics_processing" {
  source                   = "./modules/analytics_processor"
  name                     = "analytics_processor"
  input_store              = module.analytics_submission.store
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_handler_class     = "uk.nhs.nhsx.controlpanel.Handler"
  logs_bucket_id           = var.logs_bucket_id

}

module "diagnosis_keys_processing" {
  source                       = "./modules/diagnosis_keys_processor"
  submission_bucket_name       = module.diagnosis_keys_submission.store
  distribution_bucket_name     = module.diagnosis_keys_distribution_store.bucket.bucket
  distribution_id              = module.distribution_apis.distribution_id
  distribution_pattern_daily   = "/distribution/daily/*"
  distribution_pattern_2hourly = "/distribution/two-hourly/*"
  mobile_app_bundle            = var.mobile_app_bundle
  lambda_repository_bucket     = module.artifact_repository.bucket_name
  lambda_object_key            = module.artifact_repository.lambda_object_key
}

module "advanced_analytics" {
  source                     = "./modules/advanced_analytics"
  name                       = "advanced-analytics"
  lambda_handler             = "advancedAnalytics.handler"
  lambda_timeout             = 500
  analytics_submission_store = module.analytics_submission.store
  aae_environment            = var.aae_environment
}

output "analytics_processing_function" {
  value = module.analytics_processing.function
}

output "analytics_processing_output_store" {
  value = module.analytics_processing.output_store
}

output "diagnosis_keys_processing_function" {
  value = module.diagnosis_keys_processing.function
}
