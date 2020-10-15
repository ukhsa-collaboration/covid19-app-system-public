locals {
  secret_name_prefix = "/aae/dev"
  lambda_path        = "${path.module}/../../../../out/python/build/advanced_analytics"
}

data "archive_file" "this" {
  type        = "zip"
  source_dir  = local.lambda_path
  output_path = "${path.root}/../../../../out/python/${var.name}.zip"

  excludes = [
    ".pytest_cache/*",
    "__pycache__/*",
    "test/*",
    "test_*",
  ]
}

resource "aws_cloudwatch_log_group" "this" {
  name = "/aws/lambda/${var.name}"
  retention_in_days = 90
}

resource "aws_lambda_function" "this" {
  function_name                  = var.name
  runtime                        = "python3.8"
  timeout                        = var.lambda_timeout
  handler                        = var.lambda_handler
  role                           = var.iam_advanced_analytics_lambda_arn
  filename                       = data.archive_file.this.output_path
  source_code_hash               = data.archive_file.this.output_base64sha256
  reserved_concurrent_executions = 10
  depends_on                     = [aws_cloudwatch_log_group.this]

  environment {
    variables = {
      AAE_HOSTNAME = var.aae_hostname
    }
  }

  tracing_config {
    mode = "Active"
  }
}

resource "aws_lambda_permission" "to_execute" {
  statement_id  = "AllowExecutionFromS3Bucket"
  action        = "lambda:InvokeFunction"
  function_name = var.name
  principal     = "s3.amazonaws.com"
  source_arn    = "arn:aws:s3:::${var.analytics_submission_store}"

  depends_on = [aws_lambda_function.this]
}

# Attach event notification on analytics_submission bucket.
resource "aws_s3_bucket_notification" "bucket_notification" {
  bucket = var.analytics_submission_store
  lambda_function {
    lambda_function_arn = aws_lambda_function.this.arn
    events              = ["s3:ObjectCreated:*"]
    filter_suffix       = ".parquet"
  }
  depends_on = [aws_lambda_permission.to_execute]
}

resource "aws_cloudwatch_log_metric_filter" "this" {
  name           = "ErrorLogCount"
  pattern        = "[logLevel = ERROR,date,message]"
  log_group_name = aws_cloudwatch_log_group.this.name

  metric_transformation {
    name      = "${var.name}-errors"
    namespace = "ErrorLogCount"
    value     = "1"
  }
}

resource "aws_cloudwatch_log_metric_filter" "warning_lambda_metric" {
  name           = "WarningLogCount"
  pattern        = "[logLevel = WARNING,date,message]"
  log_group_name = aws_cloudwatch_log_group.this.name

  metric_transformation {
    name      = "${var.name}-warnings"
    namespace = "WarningLogCount"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "this" {
  alarm_name          = "${var.name}-Errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = aws_cloudwatch_log_metric_filter.this.metric_transformation[0].name
  namespace           = aws_cloudwatch_log_metric_filter.this.metric_transformation[0].namespace
  period              = "120"
  statistic           = "Sum"
  threshold           = "1"
  alarm_description   = "This metric monitors the error logs in Lambda ${var.name}"
  alarm_actions       = [var.app_alarms_topic]
  treat_missing_data  = "notBreaching"
}
