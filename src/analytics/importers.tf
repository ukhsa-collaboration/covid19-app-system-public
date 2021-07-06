module "coronavirus_importer" {
  source            = "./modules/coronavirus_gov_import"
  resources         = ["arn:aws:s3:::*-analytics-coronavirus-gov-*lookup-*/*"]
  statement_actions = ["*"]
  environment_variables = {
    env = var.env
  }
  lambda_function_name  = "gov_coronavirus_importer"
  log_retention_in_days = var.log_retention_in_days
  tags                  = var.tags
  app_alarms_topic      = ""
  lambda_handler_class  = "importers/coronavirus_gov_importer.handler"
  permission_set_name   = "gov_coronavirus_importer_role"
}

module "google_play_installs_importer" {
  source                   = "./modules/google_play_installs_import"
  resources                = ["*"]
  statement_actions        = ["secretsmanager:*", "s3:Put*"]
  lambda_function_name     = "google_play_installs_importer"
  log_retention_in_days    = var.log_retention_in_days
  tags                     = var.tags
  app_alarms_topic         = ""
  lambda_handler_class     = "importers/google_play_installs.handler"
  permission_set_name      = "google_play_installs_importer_role"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
}

module "ios_android_importer" {
  source                   = "./modules/ios_android_ratings_importer"
  resources                = ["*"]
  statement_actions        = ["secretsmanager:*", "s3:Put*"]
  lambda_function_name     = "ios_android_ratings_importer"
  log_retention_in_days    = var.log_retention_in_days
  tags                     = var.tags
  app_alarms_topic         = ""
  lambda_handler_class     = "importers/ios_android_ratings_importer.handler"
  permission_set_name      = "ios_android_ratings_importer_role"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  environment_variables = {
    env = var.env
  }
}

module "quicksight_users_importer" {
  source                   = "./modules/quicksight_users_importer"
  resources                = ["*"]
  statement_actions        = ["quicksight:*", "s3:Put*"]
  lambda_function_name     = "quicksight_users_importer"
  log_retention_in_days    = var.log_retention_in_days
  tags                     = var.tags
  app_alarms_topic         = ""
  lambda_handler_class     = "importers/quicksight_users_importer.handler"
  permission_set_name      = "quicksight_users_importer_role"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  environment_variables = {
    env = var.env
  }
}
