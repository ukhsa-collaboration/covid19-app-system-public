# distribution buckets require secure_origin_access to their respective origin access identity

module "exposure_configuration_distribution_access" {
  source                     = "./modules/s3_access_policies"
  policy_type                = "secure_origin_access"
  s3_bucket_arn              = module.exposure_configuration_distribution.store.arn
  origin_access_identity_arn = module.exposure_configuration_distribution.origin_access_identity_arn
}

module "post_districts_distribution_access" {
  source                     = "./modules/s3_access_policies"
  policy_type                = "cross_account_readonly"
  prefix                     = "analytics"
  principal_aws_accounts     = var.analytics_aws_accounts
  s3_bucket_arn              = module.post_districts_distribution.store.arn
  origin_access_identity_arn = module.post_districts_distribution.origin_access_identity_arn
}

module "risky_venues_distribution_access" {
  source                     = "./modules/s3_access_policies"
  policy_type                = "secure_origin_access"
  s3_bucket_arn              = module.risky_venues_distribution.store.arn
  origin_access_identity_arn = module.risky_venues_distribution.origin_access_identity_arn
}

module "self_isolation_distribution_access" {
  source                     = "./modules/s3_access_policies"
  policy_type                = "secure_origin_access"
  s3_bucket_arn              = module.self_isolation_distribution.store.arn
  origin_access_identity_arn = module.self_isolation_distribution.origin_access_identity_arn
}

module "symptomatic_questionnaire_distribution_access" {
  source                     = "./modules/s3_access_policies"
  policy_type                = "secure_origin_access"
  s3_bucket_arn              = module.symptomatic_questionnaire_distribution.store.arn
  origin_access_identity_arn = module.symptomatic_questionnaire_distribution.origin_access_identity_arn
}

module "availability_android_distribution_access" {
  source                     = "./modules/s3_access_policies"
  policy_type                = "secure_origin_access"
  s3_bucket_arn              = module.availability_android_distribution.store.arn
  origin_access_identity_arn = module.availability_android_distribution.origin_access_identity_arn
}

module "availability_ios_distribution_access" {
  source                     = "./modules/s3_access_policies"
  policy_type                = "secure_origin_access"
  s3_bucket_arn              = module.availability_ios_distribution.store.arn
  origin_access_identity_arn = module.availability_ios_distribution.origin_access_identity_arn
}

module "diagnosis_keys_distribution_store_access" {
  source                     = "./modules/s3_access_policies"
  policy_type                = "secure_origin_access"
  s3_bucket_arn              = module.diagnosis_keys_distribution_store.bucket.arn
  origin_access_identity_arn = aws_cloudfront_origin_access_identity.diagnosis_keys.iam_arn
}

module "risky_venue_configuration_distribution_access" {
  source                     = "./modules/s3_access_policies"
  policy_type                = "secure_origin_access"
  s3_bucket_arn              = module.risky_venue_configuration_distribution.store.arn
  origin_access_identity_arn = module.risky_venue_configuration_distribution.origin_access_identity_arn
}


module "empty_submission_v2_distribution_access" {
  source                     = "./modules/s3_access_policies"
  policy_type                = "secure_origin_access"
  s3_bucket_arn              = module.empty_submission_v2.store_arn
  origin_access_identity_arn = module.empty_submission_v2.origin_access_identity_arn
}
# other bucket types (submission, repository, etc.) have default secure access

module "analytics_submission_access" {
  source        = "./modules/s3_access_policies"
  policy_type   = "default"
  s3_bucket_arn = module.analytics_submission.store_arn
}

module "analytics_events_submission_access" {
  source        = "./modules/s3_access_policies"
  policy_type   = "default"
  s3_bucket_arn = module.analytics_events_submission.store_arn
}

module "diagnosis_keys_submission_access" {
  source        = "./modules/s3_access_policies"
  policy_type   = "default"
  s3_bucket_arn = module.diagnosis_keys_submission.store_arn
}

module "empty_submission_access" {
  source        = "./modules/s3_access_policies"
  policy_type   = "default"
  s3_bucket_arn = module.empty_submission.store_arn
}

module "analytics_submission_store_parquet_access" {
  source                 = "./modules/s3_access_policies"
  policy_type            = "cross_account_readonly"
  prefix                 = "analytics"
  principal_aws_accounts = var.analytics_aws_accounts
  s3_bucket_arn          = module.analytics_submission_store_parquet.bucket_arn
}

module "analytics_submission_store_parquet_access_consolidated" {
  source                 = "./modules/s3_access_policies"
  policy_type            = "cross_account_readonly"
  prefix                 = "analytics-consolidated"
  principal_aws_accounts = var.analytics_aws_accounts
  s3_bucket_arn          = module.analytics_submission_store_parquet_consolidated.bucket_arn
}

module "virology_tokens_bucket_access" {
  source        = "./modules/s3_access_policies"
  policy_type   = "default"
  s3_bucket_arn = module.virology_tokens_processing.output_store_arn
}

module "artifact_repository_access" {
  source        = "./modules/s3_access_policies"
  policy_type   = "default"
  s3_bucket_arn = module.artifact_repository.bucket_arn
}

module "exposure_notification_circuit_breaker_analytics_store_access" {
  source                 = "./modules/s3_access_policies"
  policy_type            = "cross_account_readonly"
  prefix                 = "analytics"
  principal_aws_accounts = var.analytics_aws_accounts
  s3_bucket_arn          = module.exposure_notification_circuit_breaker_analytics.analytics_bucket_arn
}

module "federation_keys_download_analytics_store_access" {
  source                 = "./modules/s3_access_policies"
  policy_type            = "cross_account_readonly"
  prefix                 = "analytics"
  principal_aws_accounts = var.analytics_aws_accounts
  s3_bucket_arn          = module.federation_keys_download_analytics.analytics_bucket_arn
}

module "federation_keys_upload_analytics_store_access" {
  source                 = "./modules/s3_access_policies"
  policy_type            = "cross_account_readonly"
  prefix                 = "analytics"
  principal_aws_accounts = var.analytics_aws_accounts
  s3_bucket_arn          = module.federation_keys_upload_analytics.analytics_bucket_arn
}

module "diagnosis_keys_analytics_store_access" {
  source                 = "./modules/s3_access_policies"
  policy_type            = "cross_account_readonly"
  prefix                 = "analytics"
  principal_aws_accounts = var.analytics_aws_accounts
  s3_bucket_arn          = module.diagnosis_keys_submission_analytics.analytics_bucket_arn
}

module "virology_test_analytics_store_access" {
  source                 = "./modules/s3_access_policies"
  policy_type            = "cross_account_readonly"
  prefix                 = "analytics"
  principal_aws_accounts = var.analytics_aws_accounts
  s3_bucket_arn          = module.log_insights_analytics_store.bucket_arn
}
