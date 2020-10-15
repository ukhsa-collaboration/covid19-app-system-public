data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

resource "aws_lambda_function" "this" {
  s3_bucket     = var.lambda_repository_bucket
  s3_key        = var.lambda_object_key
  function_name = var.lambda_function_name
  runtime       = "java11"
  timeout       = var.lambda_timeout
  memory_size   = var.lambda_memory
  handler       = var.lambda_handler_class
  role          = var.lambda_execution_role_arn
  depends_on    = [aws_cloudwatch_log_group.this]
  publish       = var.publish

  environment {
    variables = merge({ WORKSPACE = terraform.workspace, MAINTENANCE_MODE = false }, var.lambda_environment_variables)
  }

  tracing_config {
    mode = "Active"
  }
}

resource "aws_cloudwatch_log_group" "this" {
  name = "/aws/lambda/${var.lambda_function_name}"
  retention_in_days = 90 
}

resource "aws_cloudwatch_log_metric_filter" "this" {
  name           = "ErrorLogCount"
  pattern        = "[date,thread,awsRequestId,logLevel = ERROR,className, description]"
  log_group_name = aws_cloudwatch_log_group.this.name

  metric_transformation {
    name      = "${var.lambda_function_name}-errors"
    namespace = "ErrorLogCount"
    value     = "1"
  }
}

resource "aws_cloudwatch_log_metric_filter" "warning_lambda_metric" {
  name           = "WarningLogCount"
  pattern        = "[date,thread,awsRequestId,logLevel = WARN,className, description]"
  log_group_name = aws_cloudwatch_log_group.this.name

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
  alarm_actions       = [var.app_alarms_topic]
  treat_missing_data  = "notBreaching"
}

