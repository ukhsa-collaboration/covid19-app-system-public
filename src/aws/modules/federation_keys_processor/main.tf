locals {
  identifier_prefix = "${terraform.workspace}-federation-key-proc"
}

module "processor_role" {
  source = "../../libraries/iam_processing_lambda"
  name   = local.identifier_prefix

  tags = var.tags
}

module "processing_upload_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = "${local.identifier_prefix}-upload"
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.keyfederation.upload.KeyFederationUploadHandler"
  lambda_execution_role_arn = module.processor_role.arn
  lambda_timeout            = 900
  lambda_memory             = 3008
  lambda_environment_variables = {
    MAX_SUBSEQUENT_BATCH_UPLOAD_COUNT = 50
    INITIAL_UPLOAD_HISTORY_DAYS       = 14

    MAX_UPLOAD_BATCH_SIZE     = 71 // 71 mobile submissions * 14 keys (max) < 1000 keys/batch
    UPLOAD_ENABLED_WORKSPACES = join(",", var.interop_upload_enabled_workspaces)
    SUBMISSION_BUCKET_NAME    = var.submission_bucket_name
    INTEROP_BASE_URL          = var.interop_base_url

    INTEROP_AUTH_TOKEN_SECRET_NAME = "/app/interop/AuthorizationToken"
    SSM_KEY_ID_PARAMETER_NAME      = "/app/interop/SigningKeyArn"

    PROCESSOR_STATE_TABLE = aws_dynamodb_table.this.name

    REGION                        = "GB-EAW"
    FEDERATED_KEY_UPLOAD_PREFIXES = "" //This will include mobile keys by default

    # Scotland/NI/Jersey do not use transmissingRiskLevel in EN algorithms, but they set this value to 0 or 1
    UPLOAD_RISK_LEVEL_DEFAULT_ENABLED = true
    UPLOAD_RISK_LEVEL_DEFAULT         = 0
  }
  app_alarms_topic = var.alarm_topic_arn
  tags             = var.tags
}

module "processing_download_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = "${local.identifier_prefix}-download"
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.keyfederation.download.KeyFederationDownloadHandler"
  lambda_execution_role_arn = module.processor_role.arn
  lambda_timeout            = 900
  lambda_memory             = 3008
  lambda_environment_variables = {
    MAX_SUBSEQUENT_BATCH_DOWNLOAD_COUNT = 900
    INITIAL_DOWNLOAD_HISTORY_DAYS       = 14

    DOWNLOAD_ENABLED_WORKSPACES = join(",", var.interop_download_enabled_workspaces)
    SUBMISSION_BUCKET_NAME      = var.submission_bucket_name
    INTEROP_BASE_URL            = var.interop_base_url

    INTEROP_AUTH_TOKEN_SECRET_NAME = "/app/interop/AuthorizationToken"
    SSM_KEY_ID_PARAMETER_NAME      = "/app/interop/SigningKeyArn"

    PROCESSOR_STATE_TABLE         = aws_dynamodb_table.this.name
    FEDERATED_KEY_DOWNLOAD_PREFIX = "nearform"

    VALID_DOWNLOAD_ORIGINS = "JE,GB-SCT,GB-NIR,GI"

    REGION = "GB-EAW"

    DOWNLOAD_RISK_LEVEL_DEFAULT_ENABLED = true
    DOWNLOAD_RISK_LEVEL_DEFAULT         = 7
  }
  app_alarms_topic = var.alarm_topic_arn
  tags             = var.tags
}

resource "aws_dynamodb_table" "this" {
  name         = "${terraform.workspace}-federation-key-proc-history"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  tags = var.tags

  attribute {
    name = "id"
    type = "S"
  }
}

resource "aws_cloudwatch_event_rule" "every_30_mins" {
  name                = "${local.identifier_prefix}-every-30-mins"
  schedule_expression = "rate(30 minutes)"
  is_enabled          = true
  tags                = var.tags
}

resource "aws_cloudwatch_event_target" "target_upload_lambda" {
  rule = aws_cloudwatch_event_rule.every_30_mins.name
  arn  = module.processing_upload_lambda.lambda_function_arn
}

resource "aws_cloudwatch_event_target" "target_download_lambda" {
  rule = aws_cloudwatch_event_rule.every_30_mins.name
  arn  = module.processing_download_lambda.lambda_function_arn
}

resource "aws_lambda_permission" "cloudwatch_invoke_upload_lambda_permission" {
  action        = "lambda:InvokeFunction"
  function_name = module.processing_upload_lambda.lambda_function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.every_30_mins.arn
}

resource "aws_lambda_permission" "cloudwatch_invoke_download_lambda_permission" {
  action        = "lambda:InvokeFunction"
  function_name = module.processing_download_lambda.lambda_function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.every_30_mins.arn
}

resource "aws_lambda_function_event_invoke_config" "upload_lambda_function_config" {
  function_name          = module.processing_upload_lambda.lambda_function_name
  maximum_retry_attempts = 0
}

resource "aws_lambda_function_event_invoke_config" "download_lambda_function_config" {
  function_name          = module.processing_download_lambda.lambda_function_name
  maximum_retry_attempts = 0
}
