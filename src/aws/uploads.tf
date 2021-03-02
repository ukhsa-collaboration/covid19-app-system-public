locals {
  risky_venues_upload_pattern                 = "upload/identified-risk-venues"
  risky_post_districts_upload_pattern         = "upload/high-risk-postal-districts"
  test_results_upload_pattern                 = "upload/virology-test/*"
  test_results_npex_upload_pattern            = "upload/virology-test/npex-result"
  test_results_fiorano_upload_pattern         = "upload/virology-test/fiorano-result"
  test_results_eng_tokengen_upload_pattern    = "upload/virology-test/eng-result-tokengen"
  test_results_wls_tokengen_upload_pattern    = "upload/virology-test/wls-result-tokengen"
  test_results_v2_npex_upload_pattern         = "upload/virology-test/v2/npex-result"
  test_results_v2_fiorano_upload_pattern      = "upload/virology-test/v2/fiorano-result"
  test_results_v2_eng_tokengen_upload_pattern = "upload/virology-test/v2/eng-result-tokengen"
  test_results_v2_wls_tokengen_upload_pattern = "upload/virology-test/v2/wls-result-tokengen"
  test_results_health_pattern                 = "upload/virology-test/health"
  risky_venues_health_pattern                 = "upload/identified-risk-venues/health"
  risky_post_districts_health_pattern         = "upload/high-risk-postal-districts/health"
  isolation_payment_pattern                   = "/isolation-payment/ipc-token/*"
  isolation_payment_health_pattern            = "/isolation-payment/health"

  # String patterns passed to lambdas for invalidating cloudfront specific cache objects only
  risky_venues_cache_invalidation_pattern         = "/distribution/risky-venues"
  risky_post_districts_cache_invalidation_pattern = "/distribution/risky-post-districts"

  tags = merge(var.tags, {
    Component = "upload"
  })
}

resource "random_uuid" "uploads-custom-oai" {}

module "risky_venues_upload" {
  source                            = "./modules/upload"
  name                              = "risky-venues-upload"
  bucket_name                       = module.risky_venues_distribution.store.bucket
  distribution_id                   = module.distribution_apis.distribution_id
  distribution_invalidation_pattern = local.risky_venues_cache_invalidation_pattern
  lambda_repository_bucket          = module.artifact_repository.bucket_name
  lambda_object_key                 = module.artifact_repository.lambda_object_key
  lambda_handler_class              = "uk.nhs.nhsx.highriskvenuesupload.HighRiskVenuesUploadHandler"
  burst_limit                       = var.burst_limit
  rate_limit                        = var.rate_limit
  custom_oai                        = random_uuid.uploads-custom-oai.result
  log_retention_in_days             = var.log_retention_in_days
  alarm_topic_arn                   = var.alarm_topic_arn
  should_parse_additional_fields    = false
  tags                              = local.tags
}

module "risky_post_districts_upload" {
  source                            = "./modules/upload"
  name                              = "risky-post-districts-upload"
  bucket_name                       = module.post_districts_distribution.store.bucket
  distribution_id                   = module.distribution_apis.distribution_id
  distribution_invalidation_pattern = local.risky_post_districts_cache_invalidation_pattern
  lambda_repository_bucket          = module.artifact_repository.bucket_name
  lambda_object_key                 = module.artifact_repository.lambda_object_key
  lambda_handler_class              = "uk.nhs.nhsx.highriskpostcodesupload.HighRiskPostcodesUploadHandler"
  burst_limit                       = var.burst_limit
  rate_limit                        = var.rate_limit
  custom_oai                        = random_uuid.uploads-custom-oai.result
  log_retention_in_days             = var.log_retention_in_days
  alarm_topic_arn                   = var.alarm_topic_arn
  tags                              = local.tags
  invocations_alarm_enabled         = false
}

module "virology_upload" {
  source                   = "./modules/upload_virology"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  burst_limit              = var.burst_limit
  rate_limit               = var.rate_limit
  custom_oai               = random_uuid.uploads-custom-oai.result
  log_retention_in_days    = var.log_retention_in_days
  alarm_topic_arn          = var.alarm_topic_arn
  tags                     = var.tags
}
module "isolation_payment_upload" {
  source                           = "./modules/isolation_payment_upload"
  lambda_repository_bucket         = module.artifact_repository.bucket_name
  lambda_object_key                = module.artifact_repository.lambda_object_key
  burst_limit                      = var.burst_limit
  rate_limit                       = var.rate_limit
  configuration                    = lookup(var.isolation_payment, terraform.workspace, var.isolation_payment["default"])
  custom_oai                       = random_uuid.uploads-custom-oai.result
  log_retention_in_days            = var.log_retention_in_days
  alarm_topic_arn                  = var.alarm_topic_arn
  tags                             = var.tags
  dynamo_isolation_tokens_table_id = module.isolation_payment_submission.ipc_tokens_table
}
module "upload_apis" {
  source = "./libraries/cloudfront_upload_facade"

  name        = "upload"
  domain      = var.base_domain
  web_acl_arn = var.waf_arn

  risky-post-districts-upload-endpoint    = module.risky_post_districts_upload.api_endpoint
  risky-post-districts-upload-path        = local.risky_post_districts_upload_pattern
  risky-post-districts-upload-health-path = local.risky_post_districts_health_pattern
  risky-venues-upload-endpoint            = module.risky_venues_upload.api_endpoint
  risky-venues-upload-path                = local.risky_venues_upload_pattern
  risky-venues-upload-health-path         = local.risky_venues_health_pattern
  test-results-upload-endpoint            = module.virology_upload.api_endpoint
  test-results-upload-path                = local.test_results_upload_pattern
  isolation_payment_endpoint              = module.isolation_payment_upload.endpoint
  isolation_payment_path                  = local.isolation_payment_pattern
  isolation_payment_health_path           = local.isolation_payment_health_pattern
  custom_oai                              = random_uuid.uploads-custom-oai.result
  enable_shield_protection                = var.enable_shield_protection
  tags                                    = var.tags
}

output "virology_table_submission_tokens" {
  value = module.virology_upload.submission_tokens_table
}
output "virology_table_results" {
  value = module.virology_upload.results_table
}
output "virology_table_test_orders" {
  value = module.virology_upload.test_orders_table
}

output "risky_venues_upload_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.risky_venues_upload_pattern}"
}
output "risky_post_districts_upload_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.risky_post_districts_upload_pattern}"
}
output "test_results_npex_upload_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.test_results_npex_upload_pattern}"
}
output "test_results_fiorano_upload_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.test_results_fiorano_upload_pattern}"
}
output "test_results_eng_tokengen_upload_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.test_results_eng_tokengen_upload_pattern}"
}
output "test_results_wls_tokengen_upload_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.test_results_wls_tokengen_upload_pattern}"
}
output "test_results_v2_npex_upload_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.test_results_v2_npex_upload_pattern}"
}
output "test_results_v2_fiorano_upload_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.test_results_v2_fiorano_upload_pattern}"
}
output "test_results_v2_eng_tokengen_upload_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.test_results_v2_eng_tokengen_upload_pattern}"
}
output "test_results_v2_wls_tokengen_upload_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.test_results_v2_wls_tokengen_upload_pattern}"
}
output "external_facing_isolation_payment_consume_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/isolation-payment/ipc-token/consume-token"
}
output "external_facing_isolation_payment_verify_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/isolation-payment/ipc-token/verify-token"
}
output "virology_upload_lambda_function_name" {
  value = module.virology_upload.lambda_function_name
}
output "risky_post_districts_upload_lambda_function_name" {
  value = module.risky_post_districts_upload.lambda_function_name
}

output "external_facing_isolation_payment_lambda_function_name" {
  value = module.isolation_payment_upload.isolation_payment_lambda_function_name
}
# Health endpoints
output "test_results_health_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.test_results_health_pattern}"
}
output "risky_venues_upload_health_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.risky_venues_health_pattern}"
}
output "risky_post_districts_upload_health_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.risky_post_districts_health_pattern}"
}
output "external_facing_isolation_payment_health_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/${local.isolation_payment_health_pattern}"
}
# gateway endpoints

output "risky_post_districts_upload_gateway_endpoint" {
  value = module.risky_post_districts_upload.api_endpoint
}
output "test_results_upload_gateway_endpoint" {
  value = module.virology_upload.api_endpoint
}
output "external_facing_isolation_payment_endpoint" {
  value = module.isolation_payment_upload.endpoint
}
