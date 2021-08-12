module "coronavirus_importer" {
  source            = "./modules/analytics_importer"
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
  lambda_timeout        = "10"
}

module "google_play_installs_importer" {
  source            = "./modules/analytics_importer"
  resources         = ["*"]
  statement_actions = ["secretsmanager:*", "s3:Put*"]
  environment_variables = {
    secret_id          = "/google/api-key"
    target_bucket_name = "${terraform.workspace}-analytics-google-installs-overview-report"
  }
  lambda_function_name  = "google_play_installs_importer"
  log_retention_in_days = var.log_retention_in_days
  tags                  = var.tags
  app_alarms_topic      = ""
  lambda_handler_class  = "importers/google_play_installs.handler"
  permission_set_name   = "google_play_installs_importer_role"
  lambda_timeout        = "10"
}

module "apple_sales_importer" {
  source                = "./modules/analytics_importer"
  resources             = ["*"]
  statement_actions     = ["secretsmanager:*", "s3:Put*", "s3:List*"]
  lambda_function_name  = "apple_sales"
  log_retention_in_days = var.log_retention_in_days
  tags                  = var.tags
  app_alarms_topic      = ""
  lambda_handler_class  = "importers/apple_sales.handler"
  permission_set_name   = "apple_sales_importer_role"
  lambda_timeout        = "900"
  environment_variables = {
    env = var.env
  }
}

module "ios_android_importer" {
  source                = "./modules/analytics_importer"
  resources             = ["*"]
  statement_actions     = ["secretsmanager:*", "s3:Put*"]
  lambda_function_name  = "ios_android_ratings_importer"
  log_retention_in_days = var.log_retention_in_days
  tags                  = var.tags
  app_alarms_topic      = ""
  lambda_handler_class  = "importers/ios_android_ratings_importer.handler"
  permission_set_name   = "ios_android_ratings_importer_role"
  lambda_timeout        = "10"
  environment_variables = {
    env = var.env
  }
}

module "quicksight_users_importer" {
  source                = "./modules/analytics_importer"
  resources             = ["*"]
  statement_actions     = ["quicksight:*", "s3:Put*"]
  lambda_function_name  = "quicksight_users_importer"
  log_retention_in_days = var.log_retention_in_days
  tags                  = var.tags
  app_alarms_topic      = ""
  lambda_handler_class  = "importers/quicksight_users_importer.handler"
  permission_set_name   = "quicksight_users_importer_role"
  lambda_timeout        = "10"
  environment_variables = {
    env = var.env
  }
}

module "quicksight_usage_importer" {
  source                = "./modules/analytics_importer"
  resources             = ["*"]
  statement_actions     = ["cloudtrail:*", "s3:Put*", "s3:List*"]
  lambda_function_name  = "quicksight_usage_importer"
  log_retention_in_days = var.log_retention_in_days
  tags                  = var.tags
  app_alarms_topic      = ""
  lambda_handler_class  = "importers/quicksight_usage_importer.handler"
  permission_set_name   = "quicksight_usage_importer_role"
  lambda_timeout        = "900"
  environment_variables = {
    env = var.env
  }
}

module "quicksight_data_sets_importer" {
  source                = "./modules/analytics_importer"
  resources             = ["*"]
  statement_actions     = ["quicksight:*", "s3:Put*"]
  lambda_function_name  = "quicksight_data_sets_importer"
  log_retention_in_days = var.log_retention_in_days
  tags                  = var.tags
  app_alarms_topic      = ""
  lambda_handler_class  = "importers/quicksight_data_sets_importer.handler"
  permission_set_name   = "quicksight_data_sets_importer_role"
  lambda_timeout        = "600"
  environment_variables = {
    env = var.env
  }
}
