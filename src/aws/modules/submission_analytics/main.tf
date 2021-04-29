locals {
  identifier_prefix                  = "${terraform.workspace}-${var.name}-ingest"
  ingest_to_sqs_timeout_seconds      = 28
  process_submission_timeout_seconds = 25
}

module "ingest_role" {
  source = "../../libraries/iam_submission_lambda"
  name   = "${local.identifier_prefix}-sub"
  tags   = var.tags
}

data "archive_file" "ingest" {
  type        = "zip"
  source_file = "../../lambdas/analytics_ingest/handler.js"
  output_path = "../../../../out/build/analytics-ingest-lambda.zip"
}

resource "aws_lambda_function" "ingest" {
  filename         = data.archive_file.ingest.output_path
  function_name    = "${local.identifier_prefix}-sub"
  role             = module.ingest_role.arn
  handler          = "handler.ingest"
  source_code_hash = data.archive_file.ingest.output_sha
  runtime          = "nodejs12.x"
  timeout          = local.ingest_to_sqs_timeout_seconds
  memory_size      = 512
  environment {
    variables = {
      TARGET_QUEUE = aws_sqs_queue.this.id
    }
  }
  tags = var.tags
}

module "submission_gateway" {
  source               = "../../libraries/submission_api_gateway"
  name                 = "${var.name}-ingest"
  lambda_function_arn  = aws_lambda_function.ingest.arn
  lambda_function_name = aws_lambda_function.ingest.function_name
  burst_limit          = var.burst_limit
  rate_limit           = var.rate_limit
  tags                 = var.tags
}

resource "aws_sqs_queue" "this" {
  name                       = "${local.identifier_prefix}-queue"
  visibility_timeout_seconds = 180
  message_retention_seconds  = 1209600
  receive_wait_time_seconds  = 20
  tags                       = var.tags
}

module "submission_role" {
  source = "../../libraries/iam_submission_lambda"
  name   = "${local.identifier_prefix}-proc"
  tags   = var.tags
}

module "processing_lambda" {
  source                         = "../../libraries/java_lambda"
  lambda_function_name           = "${local.identifier_prefix}-proc"
  lambda_repository_bucket       = var.lambda_repository_bucket
  lambda_object_key              = var.lambda_object_key
  lambda_handler_class           = "uk.nhs.nhsx.analyticssubmission.AnalyticsSubmissionQueuedHandler"
  lambda_execution_role_arn      = module.submission_role.arn
  lambda_timeout                 = local.process_submission_timeout_seconds
  lambda_memory                  = 1024
  lambda_environment_variables   = var.lambda_environment_variables
  log_retention_in_days          = var.log_retention_in_days
  app_alarms_topic               = var.alarm_topic_arn
  reserved_concurrent_executions = var.reserved_concurrent_executions
  tags                           = var.tags
}

resource "aws_lambda_event_source_mapping" "event_source_mapping" {
  batch_size       = 1
  event_source_arn = aws_sqs_queue.this.arn
  enabled          = true
  function_name    = module.processing_lambda.lambda_function_arn
}