locals {
  identifier_prefix = "${terraform.workspace}-${var.service}-analytics"

}

module "analytics_role" {
  source = "../../libraries/iam_upload_lambda"
  name   = local.identifier_prefix
  tags   = var.tags
}

module "analytics_store" {
  source                   = "../../libraries/submission_s3"
  name                     = "analytics"
  service                  = var.service
  logs_bucket_id           = var.logs_bucket_id
  policy_document          = var.policy_document
  tags                     = var.tags
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
}

module "analytics_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = var.lambda_handler_class
  lambda_execution_role_arn = module.analytics_role.arn
  lambda_timeout            = 900
  lambda_memory             = 3008
  lambda_environment_variables = {
    ABORT_OUTSIDE_TIME_WINDOW = true
    LOG_GROUP_NAME            = var.log_group_name
    ANALYTICS_BUCKET_NAME     = module.analytics_store.bucket_name
  }
  app_alarms_topic          = var.alarm_topic_arn
  tags                      = var.tags
  invocations_alarm_enabled = false
}


resource "aws_cloudwatch_event_rule" "every_day_at_1am_utc" {
  name = "${local.identifier_prefix}-daily"

  schedule_expression = "cron(0 1 ? * * *)"
  tags                = var.tags
}
resource "aws_cloudwatch_event_target" "target_lambda" {
  rule = aws_cloudwatch_event_rule.every_day_at_1am_utc.name
  arn  = module.analytics_lambda.lambda_function_arn
}

resource "aws_lambda_permission" "cloudwatch_invoke_lambda_permission" {
  action        = "lambda:InvokeFunction"
  function_name = module.analytics_lambda.lambda_function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.every_day_at_1am_utc.arn
}


