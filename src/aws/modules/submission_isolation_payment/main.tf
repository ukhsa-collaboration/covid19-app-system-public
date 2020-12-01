locals {
  order_identifier_prefix   = "${terraform.workspace}-ipc-token-order"
  consume_identifier_prefix = "${terraform.workspace}-ipc-token-consume"
  verify_identifier_prefix  = "${terraform.workspace}-ipc-token-verify"
}

module "isolation_payment_order_role" {
  source = "../../libraries/iam_submission_lambda"
  name   = local.order_identifier_prefix
  tags   = var.tags
}

module "isolation_payment_order_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.order_identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.isolationpayment.IsolationPaymentOrderHandler"
  lambda_execution_role_arn = module.isolation_payment_order_role.arn
  lambda_timeout            = 20
  lambda_memory             = 1024
  lambda_environment_variables = {
    ISOLATION_PAYMENT_WEBSITE      = var.isolation_payment_website
    ISOLATION_PAYMENT_TOKENS_TABLE = aws_dynamodb_table.isolation_payment_tokens_table.id
    AUDIT_LOG_PREFIX               = "IPC_TOKEN_AUDIT:"
    TOKEN_EXPIRY_IN_WEEKS          = var.isolation_token_expiry_in_weeks
    COUNTRIES_WHITELISTED          = var.isolation_payment_countries_whitelisted
    SSM_KEY_ID_PARAMETER_NAME      = "/app/kms/ContentSigningKeyArn"
    custom_oai                     = var.custom_oai
    TOKEN_CREATION_ENABLED         = var.isolation_payment_token_creation_enabled
  }
  app_alarms_topic = var.alarm_topic_arn
  tags             = var.tags
}

module "isolation_payment_gateway" {
  source               = "../../libraries/submission_api_gateway"
  name                 = "isolation_payment_order_gateway"
  lambda_function_arn  = module.isolation_payment_order_lambda.lambda_function_arn
  lambda_function_name = module.isolation_payment_order_lambda.lambda_function_name
  burst_limit          = var.burst_limit
  rate_limit           = var.rate_limit
  tags                 = var.tags
}

resource "aws_dynamodb_table" "isolation_payment_tokens_table" {
  name         = "${terraform.workspace}-isolation-payment-tokens"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "tokenId"
  point_in_time_recovery {
    enabled = true
  }
  attribute {
    name = "tokenId"
    type = "S"
  }
  ttl {
    attribute_name = "expireAt"
    enabled        = true
  }
}

module "isolation_payment_gateway_role" {
  source = "../../libraries/iam_isolation_payment_lambda"
  name   = "${terraform.workspace}-isolation-payment-gateway"
  #isolation_payment_gateway_role_trust_policy_principal = [var.isolation_payment_gateway_role_trust_policy_principal]
  isolation_payment_gateway_role_trust_policy_principal = lookup(var.isolation_payment_trust_mappings, terraform.workspace, var.isolation_payment_trust_mappings["default"])
  tags                                                  = var.tags
  verify_function_arn                                   = module.isolation_payment_verify_lambda.lambda_function_arn
  consume_function_arn                                  = module.isolation_payment_consume_lambda.lambda_function_arn
}

module "isolation_payment_verify_role" {
  source = "../../libraries/iam_upload_lambda"
  name   = local.verify_identifier_prefix
  tags   = var.tags
}

module "isolation_payment_verify_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.verify_identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.isolationpayment.IsolationPaymentVerifyHandler"
  lambda_execution_role_arn = module.isolation_payment_verify_role.arn
  lambda_timeout            = 20
  lambda_memory             = 1024
  lambda_environment_variables = {
    ISOLATION_PAYMENT_TOKENS_TABLE = aws_dynamodb_table.isolation_payment_tokens_table.id
    AUDIT_LOG_PREFIX               = "IPC_TOKEN_AUDIT:"
    SUBMISSION_ENABLED             = true
    SSM_KEY_ID_PARAMETER_NAME      = "/app/kms/ContentSigningKeyArn"
    custom_oai                     = var.custom_oai
  }
  app_alarms_topic = var.alarm_topic_arn
  tags             = var.tags
}

module "isolation_payment_consume_role" {
  source = "../../libraries/iam_upload_lambda"
  name   = local.consume_identifier_prefix
  tags   = var.tags
}

module "isolation_payment_consume_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.consume_identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.isolationpayment.IsolationPaymentConsumeHandler"
  lambda_execution_role_arn = module.isolation_payment_consume_role.arn
  lambda_timeout            = 20
  lambda_memory             = 1024
  lambda_environment_variables = {
    ISOLATION_PAYMENT_TOKENS_TABLE = aws_dynamodb_table.isolation_payment_tokens_table.id
    AUDIT_LOG_PREFIX               = "IPC_TOKEN_AUDIT:"
    SUBMISSION_ENABLED             = true
    SSM_KEY_ID_PARAMETER_NAME      = "/app/kms/ContentSigningKeyArn"
    custom_oai                     = var.custom_oai
  }
  app_alarms_topic = var.alarm_topic_arn
  tags             = var.tags
}
