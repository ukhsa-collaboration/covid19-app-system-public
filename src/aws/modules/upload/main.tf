locals {
  identifier_prefix = "${terraform.workspace}-${var.name}"
}

module "upload_role" {
  source = "../../libraries/iam_upload_lambda"
  name   = local.identifier_prefix
}

module "upload_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = var.lambda_handler_class
  lambda_execution_role_arn = module.upload_role.arn
  lambda_timeout            = 20
  lambda_memory             = 1024
  lambda_environment_variables = {
    BUCKET_NAME                       = var.bucket_name,
    DISTRIBUTION_ID                   = var.distribution_id,
    SSM_KEY_ID_PARAMETER_NAME         = "/app/kms/ContentSigningKeyArn",
    DISTRIBUTION_INVALIDATION_PATTERN = var.distribution_invalidation_pattern
  }
}

module "upload_gateway" {
  source               = "../../libraries/submission_api_gateway"
  name                 = var.name
  lambda_function_arn  = module.upload_lambda.lambda_function_arn
  lambda_function_name = module.upload_lambda.lambda_function_name
  burst_limit          = var.burst_limit
  rate_limit           = var.rate_limit
}
