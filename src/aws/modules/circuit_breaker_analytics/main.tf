locals {
  identifier_prefix = "${terraform.workspace}-circuit-breaker-analytics"

}

module "circuit_breaker_analytics_role" {
  source = "../../libraries/iam_upload_lambda"
  name   = local.identifier_prefix
  tags   = var.tags
}

module "circuit_breaker_analytics_store" {
  source                   = "../../libraries/submission_s3"
  name                     = "analytics"
  service                  = "circuit-breaker"
  logs_bucket_id           = var.logs_bucket_id
  policy_document          = var.policy_document
  tags                     = var.tags
  force_destroy_s3_buckets = true
}

module "circuit_breaker_analytics_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.circuitbreakerstats.CircuitBreakerAnalyticsHandler"
  lambda_execution_role_arn = module.circuit_breaker_analytics_role.arn
  lambda_timeout            = 900
  lambda_memory             = 3008
  lambda_environment_variables = {
    ABORT_OUTSIDE_TIME_WINDOW             = true
    CIRCUIT_BREAKER_LOG_GROUP_NAME        = var.circuit_breaker_log_group_name
    CIRCUIT_BREAKER_ANALYTICS_BUCKET_NAME = module.circuit_breaker_analytics_store.bucket_name


  }
  app_alarms_topic          = var.alarm_topic_arn
  tags                      = var.tags
  invocations_alarm_enabled = false
}


resource "aws_cloudwatch_event_rule" "every_day_at_1am_utc" {
  name = "${local.identifier_prefix}-every-day-at-1am-utc"

  schedule_expression = "cron(0 1 ? * * *)"
}
resource "aws_cloudwatch_event_target" "target_lambda" {
  rule = aws_cloudwatch_event_rule.every_day_at_1am_utc.name
  arn  = module.circuit_breaker_analytics_lambda.lambda_function_arn
}

resource "aws_lambda_permission" "cloudwatch_invoke_lambda_permission" {
  action        = "lambda:InvokeFunction"
  function_name = module.circuit_breaker_analytics_lambda.lambda_function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.every_day_at_1am_utc.arn
}


