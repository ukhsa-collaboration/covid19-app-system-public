locals {
  identifier_prefix = "${terraform.workspace}-virology-tokens-proc"
}

module "processor_role" {
  source = "../../libraries/iam_processing_lambda"
  name   = local.identifier_prefix
  tags   = var.tags
}

module "virology_tokens_processing_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.virology.VirologyProcessorHandler"
  lambda_execution_role_arn = module.processor_role.arn
  lambda_timeout            = 900
  lambda_memory             = 1024
  lambda_environment_variables = {
    virology_tokens_bucket_name = module.virology_tokens_bucket.bucket_name
    test_orders_table           = var.test_orders_table_id
    test_results_table          = var.test_results_table_id
    submission_tokens_table     = var.virology_submission_tokens_table_id
    test_orders_index           = var.test_orders_index
  }
  app_alarms_topic = var.alarm_topic_arn
  tags             = var.tags
}

module "virology_tokens_bucket" {
  source         = "../../libraries/submission_s3"
  name           = "virology-tokens"
  logs_bucket_id = var.logs_bucket_id
  service        = "processing"
  tags           = var.tags
}