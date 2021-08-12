locals {
  lambda_identifier_prefix = "${terraform.workspace}-${var.lambda_function_name}"
}

module "python_lambda" {
  source                             = "../../libraries/lambda_func"
  lambda_environment_variables       = var.environment_variables
  lambda_execution_role_arn          = module.python_lambda_permission_set.arn
  lambda_function_name               = local.lambda_identifier_prefix
  lambda_handler_class               = var.lambda_handler_class
  lambda_log_group_retention_in_days = var.log_retention_in_days
  lambda_memory                      = "256"
  lambda_runtime                     = "python3.8"
  lambda_timeout                     = var.lambda_timeout
  tags                               = var.tags
  app_alarms_topic                   = var.app_alarms_topic
  invocations_alarm_enabled          = true
}

resource "aws_lambda_permission" "allow_cloudwatch" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = module.python_lambda.lambda_function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.every_day.arn
}

module "python_lambda_permission_set" {
  source            = "../../libraries/iam_lambda_func"
  name              = var.permission_set_name
  resources         = var.resources
  statement_actions = var.statement_actions
  tags              = var.tags
}

resource "aws_cloudwatch_event_rule" "every_day" {
  name                = "${local.lambda_identifier_prefix}-every-day-at-2"
  schedule_expression = "cron(0 2 * * ? *)"
  tags                = var.tags
}

resource "aws_cloudwatch_event_target" "target_lambda" {
  rule = aws_cloudwatch_event_rule.every_day.name
  arn  = module.python_lambda.lambda_function_arn
}
