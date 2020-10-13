locals {
  identifier_prefix = "${terraform.workspace}-key-proc"
}

module "processor_role" {
  source = "../../libraries/iam_processing_lambda"
  name   = local.identifier_prefix
}

module "processing_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.diagnosiskeydist.Handler"
  lambda_execution_role_arn = module.processor_role.arn
  lambda_timeout            = 900
  lambda_memory             = 3008
  lambda_environment_variables = {
    ABORT_OUTSIDE_TIME_WINDOW = true

    SSM_AG_SIGNING_KEY_ID_PARAMETER_NAME       = "/app/kms/SigningKeyArn"
    SSM_METADATA_SIGNING_KEY_ID_PARAMETER_NAME = "/app/kms/ContentSigningKeyArn"

    SUBMISSION_BUCKET_NAME = var.submission_bucket_name

    DISTRIBUTION_BUCKET_NAME          = var.distribution_bucket_name
    DISTRIBUTION_ID                   = var.distribution_id
    DISTRIBUTION_PATTERN_DAILY        = var.distribution_pattern_daily
    DISTRIBUTION_PATTERN_2HOURLY      = var.distribution_pattern_2hourly
    MOBILE_APP_BUNDLE_ID              = var.mobile_app_bundle
    DIAGNOSIS_KEY_SUBMISSION_PREFIXES = var.diagnosis_key_submission_prefixes

  }
  app_alarms_topic = var.alarm_topic_arn
}

resource "aws_cloudwatch_event_rule" "every_two_hours" {
  name                = "${local.identifier_prefix}-every-two-hours"
  schedule_expression = "cron(47 1,3,5,7,9,11,13,15,17,19,21,23 * * ? *)"
}

resource "aws_cloudwatch_event_target" "target_lambda" {
  rule = aws_cloudwatch_event_rule.every_two_hours.name
  arn  = module.processing_lambda.lambda_function_arn
}

resource "aws_lambda_permission" "cloudwatch_invoke_lambda_permission" {
  action        = "lambda:InvokeFunction"
  function_name = module.processing_lambda.lambda_function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.every_two_hours.arn
}

resource "aws_cloudwatch_metric_alarm" "duration" {
  alarm_name          = "${local.identifier_prefix}-duration"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "Duration"
  namespace           = "AWS/Lambda"
  period              = "900"
  statistic           = "Maximum"
  threshold           = "720000"
  alarm_description   = "Alarm if lambda processor is running longer than expected"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [var.alarm_topic_arn]
  dimensions = {
    FunctionName = local.identifier_prefix
  }
}

resource "aws_cloudwatch_metric_alarm" "invocations" {
  alarm_name          = "${local.identifier_prefix}-invocations"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "Invocations"
  namespace           = "AWS/Lambda"
  period              = "7200"
  statistic           = "Sum"
  threshold           = "1"
  alarm_description   = "Expect at least one invocation of lambda every two hours"
  treat_missing_data  = "breaching"
  alarm_actions       = [var.alarm_topic_arn]
  dimensions = {
    FunctionName = local.identifier_prefix
  }
}

resource "aws_cloudwatch_metric_alarm" "concurrent_executions" {
  alarm_name          = "${local.identifier_prefix}-concurrent-executions"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "ConcurrentExecutions"
  namespace           = "AWS/Lambda"
  period              = "60"
  statistic           = "Maximum"
  threshold           = "1"
  alarm_description   = "Expect no concurrent executions"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [var.alarm_topic_arn]
  dimensions = {
    FunctionName = local.identifier_prefix
  }
}
