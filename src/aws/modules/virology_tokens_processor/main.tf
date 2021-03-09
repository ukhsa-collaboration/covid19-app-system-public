locals {
  identifier_prefix           = "${terraform.workspace}-virology-tokens-proc"
  scheduled_identifier_prefix = "${terraform.workspace}-scheduled-virology-tokens-proc"
}

module "processor_role" {
  source = "../../libraries/iam_processing_lambda"
  name   = local.identifier_prefix
  tags   = var.tags
}

module "virology_tokens_processing_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.virology.VirologyProcessorHandler"
  lambda_execution_role_arn = module.processor_role.arn
  lambda_timeout            = 900
  lambda_memory             = 1024
  lambda_environment_variables = {
    virology_tokens_bucket_name = module.virology_tokens_bucket.bucket_name
    test_orders_table           = var.test_orders_table_id
    test_results_table          = var.test_results_table_id
    submission_tokens_table     = var.virology_submission_tokens_table_id
    test_orders_index           = var.test_orders_index
  }
  log_retention_in_days     = var.log_retention_in_days
  app_alarms_topic          = var.alarm_topic_arn
  tags                      = var.tags
  invocations_alarm_enabled = false
}

module "virology_tokens_bucket" {
  source          = "../../libraries/submission_s3"
  name            = "virology-tokens"
  logs_bucket_id  = var.logs_bucket_id
  service         = "processing"
  policy_document = var.policy_document
  tags            = var.tags
}

module "scheduled_virology_tokens_generating_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.scheduled_identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.virology.ScheduledCtaTokenGenerationHandler"
  lambda_execution_role_arn = module.processor_role.arn
  lambda_timeout            = 900
  lambda_memory             = 1024
  lambda_environment_variables = {
    virology_tokens_bucket_name         = module.virology_tokens_bucket.bucket_name
    test_orders_table                   = var.test_orders_table_id
    test_results_table                  = var.test_results_table_id
    submission_tokens_table             = var.virology_submission_tokens_table_id
    test_orders_index                   = var.test_orders_index
    number_of_days                      = 7
    number_of_tokens                    = 20
    number_of_batches                   = 3
    url_notification_sns_topic_arn      = module.virology_tokens_mail_topic.sns_topic_arn
    password_notification_sns_topic_arn = module.virology_tokens_sms_topic.sns_topic_arn
  }
  log_retention_in_days     = var.log_retention_in_days
  app_alarms_topic          = var.alarm_topic_arn
  tags                      = var.tags
  invocations_alarm_enabled = false
}

resource "aws_cloudwatch_event_rule" "every_week" {
  name                = "${local.scheduled_identifier_prefix}-every-week"
  schedule_expression = "cron(0 9 ? * MON *)"
  tags                = var.tags
}

resource "aws_cloudwatch_event_target" "target_lambda" {
  rule = aws_cloudwatch_event_rule.every_week.name
  arn  = module.scheduled_virology_tokens_generating_lambda.lambda_function_arn
}

resource "aws_lambda_permission" "cloudwatch_invoke_lambda_permission" {
  action        = "lambda:InvokeFunction"
  function_name = module.scheduled_virology_tokens_generating_lambda.lambda_function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.every_week.arn
}

resource "aws_lambda_function_event_invoke_config" "lambda_function_config" {
  function_name          = module.scheduled_virology_tokens_generating_lambda.lambda_function_name
  maximum_retry_attempts = 0
}

module "virology_tokens_sms_topic" {
  source = "../../libraries/sns"
  name   = "${terraform.workspace}-virology_tokens_sms_topic"
  policy_statements = [{
    sid     = "service-publish",
    actions = ["SNS:Publish"],
    effect  = "Allow",
    principals = {
      type        = "Service",
      identifiers = ["lambda.amazonaws.com"]
    },
    conditions = []
  }]
  tags = var.tags
}

module "virology_tokens_mail_topic" {
  source = "../../libraries/sns"
  name   = "${terraform.workspace}-virology_tokens_mail_topic"
  policy_statements = [{
    sid     = "service-publish",
    actions = ["SNS:Publish"],
    effect  = "Allow",
    principals = {
      type        = "Service",
      identifiers = ["lambda.amazonaws.com"]
    },
    conditions = []
  }]
  tags = var.tags
}
