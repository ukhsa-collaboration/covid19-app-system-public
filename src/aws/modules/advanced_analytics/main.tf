locals {
  function_name = "${terraform.workspace}-${var.name}"
}

data "aws_secretsmanager_secret" "private_certificate" {
  name = "/aae/advanced_analytics/private-certificate-10593"
}

data "aws_secretsmanager_secret" "private_key" {
  name = "/aae/advanced_analytics/private-key-10593"
}

data "aws_secretsmanager_secret" "certificate_encryption_password" {
  name = "/aae/advanced_analytics/certificate-encryption-password-10593"
}

data "aws_secretsmanager_secret" "subscription_key" {
  name = "/aae/advanced_analytics/subscription-key-10593"
}

module "iam_advanced_analytics_lambda" {
  source                              = "../../libraries/iam_advanced_analytics_lambda"
  bucket_name                         = var.analytics_submission_store
  certificate_secret_arn              = data.aws_secretsmanager_secret.private_certificate.arn
  encryption_password_secret_name_arn = data.aws_secretsmanager_secret.certificate_encryption_password.arn
  key_secret_name_arn                 = data.aws_secretsmanager_secret.private_key.arn
  name                                = local.function_name
  subscription_key_name_arn           = data.aws_secretsmanager_secret.subscription_key.arn
}

module "advanced_analytics_lambda" {
  source                            = "../../libraries/advanced_analytic_lambda"
  analytics_submission_store        = var.analytics_submission_store
  aae_hostname                      = var.aae_hostname
  iam_advanced_analytics_lambda_arn = module.iam_advanced_analytics_lambda.arn
  lambda_timeout                    = var.lambda_timeout
  lambda_handler                    = var.lambda_handler
  name                              = local.function_name
  app_alarms_topic                  = var.alarm_topic_arn
}
