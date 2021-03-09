locals {
  lambda_policies = [
    "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole",
    "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess",
    "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess",
    "arn:aws:iam::aws:policy/AmazonSSMReadOnlyAccess",
    "arn:aws:iam::aws:policy/SecretsManagerReadWrite"
  ]

  identifier_prefix = "${terraform.workspace}-virology"
}

module "upload_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = "${local.identifier_prefix}-upload"
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.virology.VirologyUploadHandler"
  lambda_execution_role_arn = aws_iam_role.upload_lambda_execution_role.arn
  lambda_timeout            = 20
  lambda_memory             = 1024
  lambda_environment_variables = {
    test_orders_table       = aws_dynamodb_table.test_orders.id
    test_results_table      = aws_dynamodb_table.test_results.id
    submission_tokens_table = aws_dynamodb_table.submission_tokens.id
    test_orders_index       = "${local.identifier_prefix}-ordertokens-index"
    custom_oai              = var.custom_oai
  }
  log_retention_in_days = var.log_retention_in_days
  app_alarms_topic      = var.alarm_topic_arn
  tags                  = var.tags
}

module "upload_gateway" {
  source               = "../../libraries/submission_api_gateway"
  name                 = "${local.identifier_prefix}-upload"
  lambda_function_arn  = module.upload_lambda.lambda_function_arn
  lambda_function_name = module.upload_lambda.lambda_function_name
  burst_limit          = var.burst_limit
  rate_limit           = var.rate_limit
  tags                 = var.tags
}

resource "aws_iam_role" "upload_lambda_execution_role" {
  name = "${local.identifier_prefix}-upload-lambda"

  tags = var.tags

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "upload_lambda_execution_role" {
  count      = length(local.lambda_policies)
  policy_arn = local.lambda_policies[count.index]
  role       = aws_iam_role.upload_lambda_execution_role.name
}

resource "aws_cloudwatch_metric_alarm" "Errors_4XX" {
  alarm_name          = "${module.upload_lambda.lambda_function_name}-4XXErrors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "4xx"
  namespace           = "AWS/ApiGateway"
  period              = "120"
  statistic           = "Sum"
  threshold           = "1"
  alarm_description   = "Triggers when 4xx errors occur in ${module.upload_lambda.lambda_function_name}"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [var.alarm_topic_arn]
  tags                = var.tags
  dimensions = {
    ApiId = module.upload_gateway.api_gateway_id
  }
}

resource "aws_cloudwatch_metric_alarm" "Errors_5XX" {
  alarm_name          = "${module.upload_lambda.lambda_function_name}-5XXErrors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "5xx"
  namespace           = "AWS/ApiGateway"
  period              = "120"
  statistic           = "Sum"
  threshold           = "1"
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
  threshold           = "1"
  alarm_description   = "Triggers when ${module.upload_lambda.lambda_function_name} is throttled (returns a 429)"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [var.alarm_topic_arn]
  tags                = var.tags
  dimensions = {
    FunctionName = module.upload_lambda.lambda_function_name
  }
}
