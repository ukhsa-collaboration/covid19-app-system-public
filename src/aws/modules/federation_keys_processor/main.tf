locals {
  identifier_prefix = "${terraform.workspace}-federation-key-proc"
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
  lambda_handler_class      = "uk.nhs.nhsx.keyfederation.Handler"
  lambda_execution_role_arn = module.processor_role.arn
  lambda_timeout            = 900
  lambda_memory             = 3008
  lambda_environment_variables = {
    DOWNLOAD_ENABLED                = false
    UPLOAD_ENABLED                  = false
    SUBMISSION_BUCKET_NAME          = var.submission_bucket_name
    INTEROP_BASE_URL                = var.interop_base_url
    INTEROP_AUTH_TOKEN_SECRET_NAME  = "/app/interop/AuthorizationToken"
    INTEROP_PRIVATE_KEY_SECRET_NAME = "/app/interop/PrivateKey"
    PROCESSOR_STATE_TABLE           = aws_dynamodb_table.this.name
    FEDERATED_KEY_PREFIX            = "nearform"
    VALID_REGIONS                   = "IE,GB"
    REGION                          = "GB-EAW"
  }
  app_alarms_topic = var.alarm_topic_arn
}

resource "aws_dynamodb_table" "this" {
  name         = "${terraform.workspace}-federation-key-proc-history"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }
}

resource "aws_cloudwatch_event_rule" "every_four_hours" {
  name                = "${local.identifier_prefix}-every-four-hours"
  schedule_expression = "cron(00 0,4,8,12,16,20 * * ? *)"
  is_enabled          = false
}

resource "aws_cloudwatch_event_target" "target_lambda" {
  rule = aws_cloudwatch_event_rule.every_four_hours.name
  arn  = module.processing_lambda.lambda_function_arn
}

resource "aws_lambda_permission" "cloudwatch_invoke_lambda_permission" {
  action        = "lambda:InvokeFunction"
  function_name = module.processing_lambda.lambda_function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.every_four_hours.arn
}
