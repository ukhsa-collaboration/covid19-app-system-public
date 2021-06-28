locals {
  identifier_prefix = "${terraform.workspace}-${var.name}"
}

module "upload_role" {
  source = "../../libraries/iam_upload_lambda"
  name   = local.identifier_prefix
  tags   = var.tags
}

module "upload_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = var.lambda_handler_class
  lambda_execution_role_arn = module.upload_role.arn
  lambda_timeout            = 60
  lambda_memory             = 1024
  lambda_environment_variables = {
    BUCKET_NAME                       = var.bucket_name,
    DISTRIBUTION_ID                   = var.distribution_id,
    SSM_KEY_ID_PARAMETER_NAME         = "/app/kms/ContentSigningKeyArn",
    DISTRIBUTION_INVALIDATION_PATTERN = var.distribution_invalidation_pattern
    custom_oai                        = var.custom_oai
  }
  log_retention_in_days     = var.log_retention_in_days
  app_alarms_topic          = var.alarm_topic_arn
  tags                      = var.tags
  invocations_alarm_enabled = var.invocations_alarm_enabled
}

module "upload_gateway" {
  source               = "../../libraries/submission_api_gateway"
  name                 = var.name
  lambda_function_arn  = module.upload_lambda.lambda_function_arn
  lambda_function_name = module.upload_lambda.lambda_function_name
  burst_limit          = var.burst_limit
  rate_limit           = var.rate_limit
  tags                 = var.tags
}

resource "aws_cloudwatch_metric_alarm" "Errors_5XX" {
  alarm_name          = "${module.upload_lambda.lambda_function_name}-5XXErrors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "5xx"
  namespace           = "AWS/ApiGateway"
  period              = "120"
  statistic           = "Sum"
  threshold           = "10"
  alarm_description   = "Triggers when 5xx errors occur in ${module.upload_lambda.lambda_function_name}"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [var.alarm_topic_arn]
  tags                = var.tags
  dimensions = {
    ApiId = module.upload_gateway.api_gateway_id
  }
}

resource "aws_cloudwatch_metric_alarm" "Throttles" {
  alarm_name          = "${module.upload_lambda.lambda_function_name}-Throttles"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "Throttles"
  namespace           = "AWS/Lambda"
  period              = "120"
  statistic           = "Sum"
  threshold           = "10"
  alarm_description   = "Triggers when ${module.upload_lambda.lambda_function_name} is throttled"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [var.alarm_topic_arn]
  tags                = var.tags
  dimensions = {
    FunctionName = module.upload_lambda.lambda_function_name
  }
}


