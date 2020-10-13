module "analytics_processing" {
  source                   = "./modules/analytics_processor"
  name                     = "analytics_processor"
  input_store              = module.analytics_submission_store_parquet.bucket_name
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_handler_class     = "uk.nhs.nhsx.controlpanel.Handler"
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.mobile_analytics.name
  table_name               = aws_glue_catalog_table.mobile_analytics.name
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  alarm_topic_arn          = var.alarm_topic_arn
}

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
  alarm_topic_arn                   = var.alarm_topic_arn
  diagnosis_key_submission_prefixes = ""
}

module "federation_keys_processing" {
  source                   = "./modules/federation_keys_processor"
  submission_bucket_name   = module.diagnosis_keys_submission.store
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  interop_base_url         = var.interop_base_url
  alarm_topic_arn          = var.alarm_topic_arn
}

module "advanced_analytics" {
  source                     = "./modules/advanced_analytics"
  name                       = "advanced-analytics"
  lambda_handler             = "handler.handler"
  lambda_timeout             = 500
  analytics_submission_store = module.analytics_submission_store_parquet.bucket_name
  aae_hostname               = var.aae_hostname
  alarm_topic_arn            = var.alarm_topic_arn
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
  alarm_topic_arn                     = var.alarm_topic_arn
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

output "federated_keys_processing_function" {
  value = module.federation_keys_processing.function
}

output "virology_tokens_processing_function" {
  value = module.virology_tokens_processing.function
}

output "virology_tokens_processing_output_store" {
  value = module.virology_tokens_processing.output_store
}
