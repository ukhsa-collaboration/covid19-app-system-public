locals {
  function_name      = "${terraform.workspace}-${var.name}"
  secret_name_prefix = "/aae/advanced_analytics"
  secret_names = {
    private_certificate             = "${local.secret_name_prefix}/private-certificate-10593"
    private_key                     = "${local.secret_name_prefix}/private-key-10593"
    certificate_encryption_password = "${local.secret_name_prefix}/certificate-encryption-password-10593"
    subscription_key                = "${local.secret_name_prefix}/subscription-key-10593"
  }
}

data "aws_secretsmanager_secret" "private_certificate" {
  name = local.secret_names.private_certificate
}

data "aws_secretsmanager_secret" "private_key" {
  name = local.secret_names.private_key
}

data "aws_secretsmanager_secret" "certificate_encryption_password" {
  name = local.secret_names.certificate_encryption_password
}

data "aws_secretsmanager_secret" "subscription_key" {
  name = local.secret_names.subscription_key
}

module "iam_advanced_analytics_lambda" {
  source                              = "../../libraries/iam_advanced_analytics_lambda"
  name                                = local.function_name
  bucket_name                         = var.analytics_submission_store
  certificate_secret_arn              = data.aws_secretsmanager_secret.private_certificate.arn
  key_secret_name_arn                 = data.aws_secretsmanager_secret.private_key.arn
  encryption_password_secret_name_arn = data.aws_secretsmanager_secret.certificate_encryption_password.arn
  subscription_key_name_arn           = data.aws_secretsmanager_secret.subscription_key.arn
}

module "advanced_analytics_lambda" {
  source                            = "../../libraries/advanced_analytic_lambda"
  name                              = local.function_name
  lambda_timeout                    = var.lambda_timeout
  lambda_handler                    = var.lambda_handler
  iam_advanced_analytics_lambda_arn = module.iam_advanced_analytics_lambda.arn
  analytics_submission_store        = var.analytics_submission_store
  secret_names                      = local.secret_names
  aae_environment                   = var.aae_environment
}
