locals {
  identifier_prefix = "${terraform.workspace}-${var.name}"
}


module "circuit_breaker_role" {
  source = "../../libraries/iam_circuit_breaker_lambda"
  name   = local.identifier_prefix

  tags = var.tags
}

# lambda source path will change to libraries after restructure
module "circuit_breaker_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = var.lambda_handler_class
  lambda_execution_role_arn = module.circuit_breaker_role.arn
  lambda_timeout            = 20
  lambda_memory             = 1024
  lambda_environment_variables = {
    SSM_KEY_ID_PARAMETER_NAME     = "/app/kms/ContentSigningKeyArn"
    SSM_CIRCUIT_BREAKER_BASE_NAME = "/app/${terraform.workspace}/cb"
    custom_oai                    = var.custom_oai
  }
  app_alarms_topic = var.alarm_topic_arn
  tags             = var.tags
}

module "circuit_breaker_gateway" {
  source               = "../../libraries/circuit_breaker_api_gateway"
  name                 = var.name
  lambda_function_arn  = module.circuit_breaker_lambda.lambda_function_arn
  lambda_function_name = module.circuit_breaker_lambda.lambda_function_name
  burst_limit          = var.burst_limit
  rate_limit           = var.rate_limit
  tags                 = var.tags
}

resource "aws_ssm_parameter" "initial" {
  name  = "/app/${terraform.workspace}/cb/${var.ssm_parameter}-initial"
  type  = "String"
  value = "YES"
}

resource "aws_ssm_parameter" "poll" {
  name  = "/app/${terraform.workspace}/cb/${var.ssm_parameter}-poll"
  type  = "String"
  value = "YES"
}
