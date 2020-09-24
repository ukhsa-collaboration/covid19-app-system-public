locals {
  identifier_prefix             = "${terraform.workspace}-${var.name}-proc"
  identifier_prefix_underscores = "${terraform.workspace}_${var.name}_proc"
}

module "processor_role" {
  source = "../../libraries/iam_processing_lambda"
  name   = local.identifier_prefix
}

module "output_store" {
  source                   = "../../libraries/analytics_s3"
  name                     = "processing-store"
  service                  = "analytics"
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
}

module "athena" {
  source           = "../../libraries/athena"
  database_name    = var.database_name
  s3_input_bucket  = var.input_store
  s3_output_bucket = module.output_store.bucket_name
  table_name       = var.table_name
}

module "processing_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_execution_role_arn = module.processor_role.arn
  lambda_function_name      = local.identifier_prefix
  lambda_handler_class      = var.lambda_handler_class
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_memory             = 3008
  lambda_timeout            = 900
  lambda_environment_variables = {
    DATABASE_NAME = var.database_name
    INPUT_BUCKET  = var.input_store
    OUTPUT_BUCKET = module.output_store.bucket_name
    WORKGROUP     = module.athena.workgroup_name
  }
  app_alarms_topic = var.alarm_topic_arn
}

resource "aws_cloudwatch_event_rule" "cron" {
  name                = "${local.identifier_prefix}-cron"
  schedule_expression = "cron(12 1,5,7,9,11,13,15,17,19,21 * * ? *)"
}

resource "aws_cloudwatch_event_target" "target_lambda" {
  rule = aws_cloudwatch_event_rule.cron.name
  arn  = module.processing_lambda.lambda_function_arn
}

resource "aws_lambda_permission" "allow_cloudwatch_call" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = module.processing_lambda.lambda_function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.cron.arn
}
