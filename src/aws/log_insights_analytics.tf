module "exposure_notification_circuit_breaker_analytics" {
  source                   = "./modules/log_insights_analytics"
  service                  = "en-circuit-breaker"
  alarm_topic_arn          = var.alarm_topic_arn
  log_group_name           = module.exposure_notification_circuit_breaker.lambda_log_group
  lambda_handler_class     = "uk.nhs.nhsx.analyticslogs.CircuitBreakerAnalyticsHandler"
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_repository_bucket = module.artifact_repository.bucket_name
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
  policy_document          = module.exposure_notification_circuit_breaker_analytics_store_access.policy_document
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
}

module "federation_keys_download_analytics" {
  source                   = "./modules/log_insights_analytics"
  service                  = "federation-key-download"
  alarm_topic_arn          = var.alarm_topic_arn
  log_group_name           = module.federation_keys_processing.download_lambda_log_group
  lambda_handler_class     = "uk.nhs.nhsx.analyticslogs.KeyFederationDownloadAnalyticsHandler"
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_repository_bucket = module.artifact_repository.bucket_name
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
  policy_document          = module.federation_keys_download_analytics_store_access.policy_document
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
}

module "federation_keys_upload_analytics" {
  source                   = "./modules/log_insights_analytics"
  service                  = "federation-key-upload"
  alarm_topic_arn          = var.alarm_topic_arn
  log_group_name           = module.federation_keys_processing.upload_lambda_log_group
  lambda_handler_class     = "uk.nhs.nhsx.analyticslogs.KeyFederationUploadAnalyticsHandler"
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_repository_bucket = module.artifact_repository.bucket_name
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
  policy_document          = module.federation_keys_upload_analytics_store_access.policy_document
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
}

module "diagnosis_keys_submission_analytics" {
  source                   = "./modules/log_insights_analytics"
  service                  = "diagnosis-keys-submission"
  alarm_topic_arn          = var.alarm_topic_arn
  log_group_name           = module.diagnosis_keys_submission.log_group
  lambda_handler_class     = "uk.nhs.nhsx.analyticslogs.DiagnosisKeysSubmissionStatsAnalyticsHandler"
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_repository_bucket = module.artifact_repository.bucket_name
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
  policy_document          = module.diagnosis_keys_analytics_store_access.policy_document
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
}

module "cta_token_gen_analytics" {
  source                   = "./modules/log_insights_analytics_v2"
  service                  = "cta-token-gen"
  alarm_topic_arn          = var.alarm_topic_arn
  log_group_name           = module.virology_upload.log_group
  lambda_handler_class     = "uk.nhs.nhsx.analyticslogs.CtaTokenGenAnalyticsHandler"
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_repository_bucket = module.artifact_repository.bucket_name
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
  policy_document          = module.virology_test_analytics_store_access.policy_document
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  analytics_bucket_name    = module.log_insights_analytics_store.bucket_name
  analytics_bucket_prefix  = "cta-token-gen"
}

module "cta_exchange_analytics" {
  source                   = "./modules/log_insights_analytics_v2"
  service                  = "cta-exchange"
  alarm_topic_arn          = var.alarm_topic_arn
  log_group_name           = module.virology_submission.log_group
  lambda_handler_class     = "uk.nhs.nhsx.analyticslogs.CtaExchangeAnalyticsHandler"
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_repository_bucket = module.artifact_repository.bucket_name
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
  policy_document          = module.virology_test_analytics_store_access.policy_document
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  analytics_bucket_name    = module.log_insights_analytics_store.bucket_name
  analytics_bucket_prefix  = "cta-exchange"
}

module "cta_token_status_analytics" {
  source                   = "./modules/log_insights_analytics_v2"
  service                  = "cta-token-status"
  alarm_topic_arn          = var.alarm_topic_arn
  log_group_name           = module.virology_upload.log_group
  lambda_handler_class     = "uk.nhs.nhsx.analyticslogs.CtaTokenStatusAnalyticsHandler"
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_repository_bucket = module.artifact_repository.bucket_name
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
  policy_document          = module.virology_test_analytics_store_access.policy_document
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  analytics_bucket_name    = module.log_insights_analytics_store.bucket_name
  analytics_bucket_prefix  = "cta-token-status"
}

module "log_insights_analytics_store" {
  source                   = "./libraries/submission_s3"
  name                     = "analytics"
  service                  = "log-insights"
  logs_bucket_id           = var.logs_bucket_id
  policy_document          = module.virology_test_analytics_store_access.policy_document
  tags                     = var.tags
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
}

output "exposure_notification_circuit_breaker_analytics_lambda_function_name" {
  value = module.exposure_notification_circuit_breaker_analytics.lambda_function_name
}

output "federation_keys_download_analytics_lambda_function_name" {
  value = module.federation_keys_download_analytics.lambda_function_name
}

output "federation_keys_upload_analytics_lambda_function_name" {
  value = module.federation_keys_upload_analytics.lambda_function_name
}
