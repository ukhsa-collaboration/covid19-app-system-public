module "edge_analytics" {
  source                                    = "./modules/edge_analytics"
  query_completion_polling_interval_seconds = var.query_completion_polling_interval_seconds
  tags                                      = var.tags
  logs_bucket_id                            = var.logs_bucket_id
  log_retention_in_days                     = var.log_retention_in_days
  force_destroy_s3_buckets                  = var.force_destroy_s3_buckets
  lambda_repository_bucket                  = module.artifact_repository.bucket_name
  lambda_object_key                         = module.artifact_repository.lambda_object_key
  alarm_topic_arn                           = var.alarm_topic_arn
  edge_export_url                           = var.edge_export_url
  enabled_workspaces                        = var.edge_export_mobile_analytics_enabled_workspaces
  mobile_analytics_table                    = "analytics_mobile"
}
