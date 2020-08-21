module "analytics_submission" {
  source                       = "./modules/submission"
  name                         = "analytics"
  lambda_repository_bucket     = module.artifact_repository.bucket_name
  lambda_object_key            = module.artifact_repository.lambda_object_key
  lambda_handler_class         = "uk.nhs.nhsx.analyticssubmission.Handler"
  lambda_environment_variables = {}
  burst_limit                  = var.burst_limit
  rate_limit                   = var.rate_limit
  logs_bucket_id               = var.logs_bucket_id
}

module "diagnosis_keys_submission" {
  source                   = "./modules/submission"
  name                     = "diagnosis-keys"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_handler_class     = "uk.nhs.nhsx.diagnosiskeyssubmission.Handler"
  lambda_environment_variables = {
    SSM_KEY_ID_PARAMETER_NAME = "/app/kms/ContentSigningKeyArn"
    submission_tokens_table   = module.virology_upload.submission_tokens_table
  }
  burst_limit    = var.burst_limit
  rate_limit     = var.rate_limit
  logs_bucket_id = var.logs_bucket_id
}

module "activation_keys_submission" {
  source                   = "./modules/submission_activation_keys"
  name                     = "activation-keys"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  burst_limit              = var.burst_limit
  rate_limit               = var.rate_limit
}

module "virology_submission" {
  source                              = "./modules/submission_virology"
  lambda_repository_bucket            = module.artifact_repository.bucket_name
  lambda_object_key                   = module.artifact_repository.lambda_object_key
  burst_limit                         = var.burst_limit
  rate_limit                          = var.rate_limit
  test_order_website                  = var.virology_test_order_website
  test_register_website               = var.virology_test_register_website
  test_orders_table_id                = module.virology_upload.test_orders_table
  test_results_table_id               = module.virology_upload.results_table
  virology_submission_tokens_table_id = module.virology_upload.submission_tokens_table
}

module "submission_apis" {
  source = "./libraries/cloudfront_submission_facade"

  name        = "submission"
  domain      = var.base_domain
  web_acl_arn = data.aws_wafv2_web_acl.this.arn

  analytics_submission_endpoint                  = module.analytics_submission.endpoint
  analytics_submission_path                      = "/submission/mobile-analytics"
  diagnosis_keys_submission_endpoint             = module.diagnosis_keys_submission.endpoint
  diagnosis_keys_submission_path                 = "/submission/diagnosis-keys"
  exposure_notification_circuit_breaker_endpoint = module.exposure_notification_circuit_breaker.endpoint
  exposure_notification_circuit_breaker_path     = "/circuit-breaker/exposure-notification/*"
  risky_venues_circuit_breaker_endpoint          = module.risky_venues_circuit_breaker.endpoint
  risky_venues_circuit_breaker_path              = "/circuit-breaker/venue/*"
  virology_kit_endpoint                          = module.virology_submission.api_endpoint
  virology_kit_path                              = "/virology-test/*"
  activation_keys_submission_endpoint            = module.activation_keys_submission.endpoint
  activation_keys_submission_path                = "/activation/request"
}

output "analytics_submission_store" {
  value = module.analytics_submission.store
}
output "diagnosis_keys_submission_store" {
  value = module.diagnosis_keys_submission.store
}
output "analytics_submission_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/mobile-analytics"
}
output "diagnosis_keys_submission_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/diagnosis-keys"
}
output "virology_kit_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/virology-test"
}
output "activation_keys_submission_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/activation/request"
}
