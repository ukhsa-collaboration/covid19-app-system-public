locals {
  aae_export_is_enabled = contains(var.aae_export_enabled_workspaces, terraform.workspace)
}

module "advanced_analytics_export" {
  count                    = local.aae_export_is_enabled ? 1 : 0
  name                     = "aae-mobile-analytics-parquet-export"
  source                   = "./modules/advanced_analytics_export"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  log_retention_in_days    = var.log_retention_in_days
  alarm_topic_arn          = var.alarm_topic_arn
  tags                     = var.tags


  # [PARQUET CONSOLIDATION] FIXME below: the following configuration must be active during the initial migration (parquet consolidation)
  analytics_submission_store = var.analytics_submission_store_parquet_bucket_id

  # [PARQUET CONSOLIDATION] FIXME below: the following configuration must be activated before the delta migration (parquet consolidation) is started
  # analytics_submission_store = module.analytics_submission_store_parquet_consolidated.bucket_name
  aae_url_prefix                = var.aae_mobile_analytics_url_prefix
  aae_url_suffix                = var.aae_mobile_analytics_url_suffix
  p12_cert_secret_name          = var.aae_mobile_analytics_p12_cert_secret_name
  p12_cert_password_secret_name = var.aae_mobile_analytics_p12_cert_password_secret_name
  aae_subscription_secret_name  = var.aae_mobile_analytics_subscription_secret_name
}

module "advanced_analytics_events_export" {
  count                         = local.aae_export_is_enabled ? 1 : 0
  name                          = "aae-mobile-analytics-events-json-export"
  source                        = "./modules/advanced_analytics_export"
  lambda_repository_bucket      = module.artifact_repository.bucket_name
  lambda_object_key             = module.artifact_repository.lambda_object_key
  log_retention_in_days         = var.log_retention_in_days
  alarm_topic_arn               = var.alarm_topic_arn
  tags                          = var.tags
  analytics_submission_store    = var.analytics_submission_events_bucket_id
  aae_url_prefix                = var.aae_mobile_analytics_events_url_prefix
  aae_url_suffix                = var.aae_mobile_analytics_events_url_suffix
  p12_cert_secret_name          = var.aae_mobile_analytics_events_p12_cert_secret_name
  p12_cert_password_secret_name = var.aae_mobile_analytics_events_p12_cert_password_secret_name
  aae_subscription_secret_name  = var.aae_mobile_analytics_events_subscription_secret_name
}
