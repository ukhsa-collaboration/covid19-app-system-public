locals {
  risky_venues_upload_pattern         = "upload/identified-risk-venues"
  risky_post_districts_upload_pattern = "upload/high-risk-postal-districts"

  # String patterns passed to lambdas for invalidating cloudfront specific cache objects only
  risky_venues_cache_invalidation_pattern         = "/distribution/risky-venues"
  risky_post_districts_cache_invalidation_pattern = "/distribution/risky-post-districts"
}

module "risky_venues_upload" {
  source                            = "./modules/upload"
  name                              = "risky-venues-upload"
  bucket_name                       = module.risky_venues_distribution.store.bucket
  distribution_id                   = module.distribution_apis.distribution_id
  distribution_invalidation_pattern = local.risky_venues_cache_invalidation_pattern
  lambda_repository_bucket          = module.artifact_repository.bucket_name
  lambda_object_key                 = module.artifact_repository.lambda_object_key
  lambda_handler_class              = "uk.nhs.nhsx.highriskvenuesupload.Handler"
  burst_limit                       = var.burst_limit
  rate_limit                        = var.rate_limit
}

module "risky_post_districts_upload" {
  source                            = "./modules/upload"
  name                              = "risky-post-districts-upload"
  bucket_name                       = module.post_districts_distribution.store.bucket
  distribution_id                   = module.distribution_apis.distribution_id
  distribution_invalidation_pattern = local.risky_post_districts_cache_invalidation_pattern
  lambda_repository_bucket          = module.artifact_repository.bucket_name
  lambda_object_key                 = module.artifact_repository.lambda_object_key
  lambda_handler_class              = "uk.nhs.nhsx.highriskpostcodesupload.Handler"
  burst_limit                       = var.burst_limit
  rate_limit                        = var.rate_limit
}

module "virology_upload" {
  source                   = "./modules/upload_virology"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  burst_limit              = var.burst_limit
  rate_limit               = var.rate_limit
}

module "upload_apis" {
  source = "./libraries/cloudfront_upload_facade"

  name        = "upload"
  domain      = var.base_domain
  web_acl_arn = data.aws_wafv2_web_acl.this.arn

  risky-post-districts-upload-endpoint = module.risky_post_districts_upload.api_endpoint
  risky-post-districts-upload-path     = local.risky_post_districts_upload_pattern
  risky-venues-upload-endpoint         = module.risky_venues_upload.api_endpoint
  risky-venues-upload-path             = local.risky_venues_upload_pattern
  test-results-upload-endpoint         = module.virology_upload.api_endpoint
  test-results-upload-path             = "/upload/virology-test/npex-result"
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
output "test_results_upload_endpoint" {
  value = "https://${module.upload_apis.upload_domain_name}/upload/virology-test/npex-result"
}
