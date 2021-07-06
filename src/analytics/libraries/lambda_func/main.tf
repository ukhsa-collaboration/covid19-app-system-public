locals {
  source_dir        = "${path.module}/../../../../out/analytics/tmp"
  identifier_prefix = "${terraform.workspace}-${var.lambda_function_name}"
}

data "archive_file" "lambda_importer" {
  type        = "zip"
  output_path = "${path.module}/../../../../out/python_package/lambda_importers.zip"
  source_dir  = local.source_dir
  excludes    = ["requirements.txt"]
}

resource "aws_lambda_function" "this" {
  filename         = data.archive_file.lambda_importer.output_path
  source_code_hash = filebase64sha256(data.archive_file.lambda_importer.output_path)
  function_name    = local.identifier_prefix
  handler          = var.lambda_handler_class
  runtime          = var.lambda_runtime
  timeout          = var.lambda_timeout
  memory_size      = var.lambda_memory
  role             = var.lambda_execution_role_arn
  tags             = var.tags

  tracing_config {
    mode = "Active"
  }

  environment {
    variables = var.lambda_environment_variables
  }
}

resource "aws_cloudwatch_log_group" "lambda_log_group" {
  name              = "/aws/lambda/${var.lambda_function_name}"
  retention_in_days = var.lambda_log_group_retention_in_days
  tags              = var.tags
}

data "aws_region" "current" {}

data "aws_caller_identity" "current" {}


resource "aws_cloudwatch_log_metric_filter" "this" {
  name           = "ErrorLogCount"
  pattern        = "{($.metadata.category = \"ERROR\")}"
  log_group_name = aws_cloudwatch_log_group.lambda_log_group.name

  metric_transformation {
    name      = "${var.lambda_function_name}-errors"
    namespace = "ErrorLogCount"
    value     = "1"
  }
}

resource "aws_cloudwatch_log_metric_filter" "warning_lambda_metric" {
  name           = "WarningLogCount"
  pattern        = "{($.metadata.category = \"WARNING\")}"
  log_group_name = aws_cloudwatch_log_group.lambda_log_group.name

  metric_transformation {
    name      = "${var.lambda_function_name}-warnings"
    namespace = "WarningLogCount"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "this" {
  alarm_name          = "${var.lambda_function_name}-Errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = aws_cloudwatch_log_metric_filter.this.metric_transformation[0].name
  namespace           = aws_cloudwatch_log_metric_filter.this.metric_transformation[0].namespace
  period              = "120"
  statistic           = "Sum"
  threshold           = "1"
  alarm_description   = "This metric monitors the error logs in Lambda ${var.lambda_function_name}"
  //  alarm_actions       = [var.app_alarms_topic]
  treat_missing_data = "notBreaching"
  tags               = var.tags
}

resource "aws_cloudwatch_metric_alarm" "invocations" {
  count               = var.invocations_alarm_enabled ? 1 : 0
  alarm_name          = "${var.lambda_function_name}-invocations"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "Invocations"
  namespace           = "AWS/Lambda"
  period              = "86400"
  statistic           = "Sum"
  threshold           = "1"
  alarm_description   = "Expect at least one invocation of lambda every 24 hours"
  treat_missing_data  = "breaching"
  //  alarm_actions       = [var.app_alarms_topic]
  tags = var.tags
  dimensions = {
    FunctionName = var.lambda_function_name
  }
}
