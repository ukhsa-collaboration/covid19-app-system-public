module "diagnosis_keys_processing" {
  source                            = "./modules/diagnosis_keys_processor"
  submission_bucket_name            = module.diagnosis_keys_submission.store
  distribution_bucket_name          = module.diagnosis_keys_distribution_store.bucket.bucket
  distribution_id                   = module.distribution_apis.distribution_id
  distribution_pattern_daily        = "/distribution/daily/*"
  distribution_pattern_2hourly      = "/distribution/two-hourly/*"
  mobile_app_bundle                 = var.mobile_app_bundle
  lambda_repository_bucket          = module.artifact_repository.bucket_name
  lambda_object_key                 = module.artifact_repository.lambda_object_key
  log_retention_in_days             = var.log_retention_in_days
  alarm_topic_arn                   = var.alarm_topic_arn
  diagnosis_key_submission_prefixes = "nearform/JE,nearform/GB-SCT,nearform/GB-NIR,nearform/GI"
  zip_submission_period_offset      = lookup(var.zip_submission_period_offset, terraform.workspace, var.zip_submission_period_offset["default"])
  tags                              = var.tags
}

module "federation_keys_processing" {
  source                              = "./modules/federation_keys_processor"
  submission_bucket_name              = module.diagnosis_keys_submission.store
  lambda_repository_bucket            = module.artifact_repository.bucket_name
  lambda_object_key                   = module.artifact_repository.lambda_object_key
  interop_base_url                    = var.interop_base_url
  interop_download_enabled_workspaces = var.interop_download_enabled_workspaces
  interop_upload_enabled_workspaces   = var.interop_upload_enabled_workspaces
  log_retention_in_days               = var.log_retention_in_days
  alarm_topic_arn                     = var.alarm_topic_arn
  tags                                = var.tags
}

module "advanced_analytics_export" {
  name                          = "aae-mobile-analytics-parquet-export"
  source                        = "./modules/advanced_analytics_export"
  lambda_repository_bucket      = module.artifact_repository.bucket_name
  lambda_object_key             = module.artifact_repository.lambda_object_key
  log_retention_in_days         = var.log_retention_in_days
  alarm_topic_arn               = var.alarm_topic_arn
  tags                          = var.tags
  analytics_submission_store    = module.analytics_submission_store_parquet.bucket_name
  enabled_workspaces            = var.aae_mobile_analytics_enabled_workspaces
  aae_url_prefix                = var.aae_mobile_analytics_url_prefix
  aae_url_suffix                = var.aae_mobile_analytics_url_suffix
  p12_cert_secret_name          = var.aae_mobile_analytics_p12_cert_secret_name
  p12_cert_password_secret_name = var.aae_mobile_analytics_p12_cert_password_secret_name
  aae_subscription_secret_name  = var.aae_mobile_analytics_subscription_secret_name
}

module "advanced_analytics_events_export" {
  name                          = "aae-mobile-analytics-events-json-export"
  source                        = "./modules/advanced_analytics_export"
  lambda_repository_bucket      = module.artifact_repository.bucket_name
  lambda_object_key             = module.artifact_repository.lambda_object_key
  log_retention_in_days         = var.log_retention_in_days
  alarm_topic_arn               = var.alarm_topic_arn
  tags                          = var.tags
  analytics_submission_store    = module.analytics_events_submission.store
  enabled_workspaces            = var.aae_mobile_analytics_events_enabled_workspaces
  aae_url_prefix                = var.aae_mobile_analytics_events_url_prefix
  aae_url_suffix                = var.aae_mobile_analytics_events_url_suffix
  p12_cert_secret_name          = var.aae_mobile_analytics_events_p12_cert_secret_name
  p12_cert_password_secret_name = var.aae_mobile_analytics_events_p12_cert_password_secret_name
  aae_subscription_secret_name  = var.aae_mobile_analytics_events_subscription_secret_name
}

module "virology_tokens_processing" {
  source                              = "./modules/virology_tokens_processor"
  lambda_repository_bucket            = module.artifact_repository.bucket_name
  lambda_object_key                   = module.artifact_repository.lambda_object_key
  test_orders_table_id                = module.virology_upload.test_orders_table
  test_results_table_id               = module.virology_upload.results_table
  virology_submission_tokens_table_id = module.virology_upload.submission_tokens_table
  test_orders_index                   = module.virology_upload.test_orders_index_name
  logs_bucket_id                      = var.logs_bucket_id
  log_retention_in_days               = var.log_retention_in_days
  alarm_topic_arn                     = var.alarm_topic_arn
  policy_document                     = module.virology_tokens_bucket_access.policy_document
  tags                                = var.tags
}

output "aae_events_export_function_name" {
  value = module.advanced_analytics_events_export.lambda_function_name
}

output "aae_events_export_function_arn" {
  value = module.advanced_analytics_events_export.lambda_function_arn
}

output "aae_events_export_event_source_arn" {
  value = module.advanced_analytics_events_export.event_source_arn
}

output "aae_export_function_name" {
  value = module.advanced_analytics_export.lambda_function_name
}

output "aae_export_function_arn" {
  value = module.advanced_analytics_export.lambda_function_arn
}

output "aae_export_event_source_arn" {
  value = module.advanced_analytics_export.event_source_arn
}

output "diagnosis_keys_processing_function" {
  value = module.diagnosis_keys_processing.function
}

output "federation_keys_processing_upload_function" {
  value = module.federation_keys_processing.upload_lambda_function
}

output "federation_keys_processing_download_function" {
  value = module.federation_keys_processing.download_lambda_function
}

output "virology_tokens_processing_function" {
  value = module.virology_tokens_processing.function
}

output "virology_tokens_processing_output_store" {
  value = module.virology_tokens_processing.output_store
}

output "virology_tokens_processing_sms_topic_arn" {
  value = module.virology_tokens_processing.sms_topic_arn
}

output "virology_tokens_processing_email_topic_arn" {
  value = module.virology_tokens_processing.email_topic_arn
}
