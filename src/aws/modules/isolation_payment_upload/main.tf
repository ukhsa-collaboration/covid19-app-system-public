locals {
  api_identifier_prefix = "${terraform.workspace}-ipc-token-api"
}
module "isolation_payment_api_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.api_identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.isolationpayment.IsolationPaymentUploadHandler"
  lambda_execution_role_arn = module.isolation_payment_role.arn
  lambda_timeout            = 20
  lambda_memory             = 1024
  lambda_environment_variables = {
    ISOLATION_PAYMENT_TOKENS_TABLE = var.dynamo_isolation_tokens_table_id
    AUDIT_LOG_PREFIX               = "IPC_TOKEN_AUDIT:"
    COUNTRIES_WHITELISTED          = var.configuration["countries_whitelisted"]
    SSM_KEY_ID_PARAMETER_NAME      = "/app/kms/ContentSigningKeyArn"
    custom_oai                     = var.custom_oai
    TOKEN_CREATION_ENABLED         = var.configuration["enabled"]
  }
  log_retention_in_days = var.log_retention_in_days
  app_alarms_topic      = var.alarm_topic_arn
  tags                  = var.tags
}
module "isolation_payment_role" {
  source = "../../libraries/iam_upload_lambda"
  name   = local.api_identifier_prefix
  tags   = var.tags
}
module "isolation_payment_api" {
  source               = "../../libraries/submission_api_gateway"
  name                 = "isolation_payment_api"
  lambda_function_arn  = module.isolation_payment_api_lambda.lambda_function_arn
  lambda_function_name = module.isolation_payment_api_lambda.lambda_function_name
  burst_limit          = var.burst_limit
  rate_limit           = var.rate_limit
  tags                 = var.tags
}
