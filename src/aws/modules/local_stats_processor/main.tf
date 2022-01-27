locals {
  identifier_prefix = "${terraform.workspace}-local-stats-proc"
}

module "processor_role" {
  source = "../../libraries/iam_processing_lambda"
  name   = local.identifier_prefix
  tags   = var.tags
}

module "processing_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.localstats.handler.DailyLocalStatsHandler"
  lambda_execution_role_arn = module.processor_role.arn
  lambda_timeout            = 900
  lambda_memory             = 2048
  lambda_environment_variables = {
    LOCAL_STATS_BUCKET_NAME   = var.distribution_bucket_name,
    SSM_KEY_ID_PARAMETER_NAME = "/app/kms/ContentSigningKeyArn"
  }
  log_retention_in_days     = var.log_retention_in_days
  app_alarms_topic          = var.alarm_topic_arn
  tags                      = var.tags
  invocations_alarm_enabled = false
}

resource "aws_cloudwatch_event_rule" "every_hour" {
  name                = "${local.identifier_prefix}-every-hour"
  schedule_expression = "cron(30 0/1 * * ? *)"
  is_enabled          = var.is_enabled
  tags                = var.tags
}

resource "aws_cloudwatch_event_target" "target_lambda" {
  rule = aws_cloudwatch_event_rule.every_hour.name
  arn  = module.processing_lambda.lambda_function_arn
}

resource "aws_lambda_permission" "cloudwatch_invoke_lambda_permission" {
  action        = "lambda:InvokeFunction"
  function_name = module.processing_lambda.lambda_function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.every_hour.arn
}

resource "aws_lambda_function_event_invoke_config" "lambda_function_config" {
  function_name          = module.processing_lambda.lambda_function_name
  maximum_retry_attempts = 0
}
