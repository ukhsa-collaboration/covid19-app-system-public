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

  environment {
    variables = merge({ WORKSPACE = terraform.workspace }, var.lambda_environment_variables)
  }

  tracing_config {
    mode = "Active"
  }
}

resource "aws_cloudwatch_log_group" "this" {
  name = "/aws/lambda/${var.lambda_function_name}"
}

resource "aws_cloudwatch_log_metric_filter" "this" {
  name           = "ErrorLogCount"
  pattern        = "ERROR"
  log_group_name = aws_cloudwatch_log_group.this.name

  metric_transformation {
    name      = "${var.lambda_function_name}-errors"
    namespace = "ErrorLogCount"
    value     = "1"
  }
}

resource "aws_cloudwatch_log_metric_filter" "warning_lambda_metric" {
  name           = "WarningLogCount"
  pattern        = "WARN"
  log_group_name = aws_cloudwatch_log_group.this.name

  metric_transformation {
    name      = "${var.lambda_function_name}-warnings"
    namespace = "WarningLogCount"
    value     = "1"
  }
}