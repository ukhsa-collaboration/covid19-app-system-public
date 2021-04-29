locals {
  service = "canary"
}

module "lambda_execution_role" {
  region  = var.canary_deploy_region
  source  = "../aws/libraries/iam_synthetic_canary"
  service = local.service
  name    = "runner"
  tags    = var.tags
}

module "submission_probes" {
  source               = "../aws/modules/synthetics_submission"
  service              = "submissions"
  base_domain          = var.base_domain
  region               = var.canary_deploy_region
  lambda_exec_role_arn = module.lambda_execution_role.role.arn
  logs_bucket_id       = var.logs_bucket_id
  tags                 = var.tags
}

module "upload_probes" {
  // Upload probes do not work in Staging or Prod because of WAF origin-IP filtering rules
  enabled              = var.aws_account_name == "dev"
  source               = "../aws/modules/synthetics_upload"
  service              = "uploads"
  base_domain          = var.base_domain
  region               = var.canary_deploy_region
  lambda_exec_role_arn = module.lambda_execution_role.role.arn
  logs_bucket_id       = var.logs_bucket_id
  dependency_ref       = module.submission_probes.probe_virology_test_function_name # create canaries one at a time to avoid 429 errors
  tags                 = var.tags
}

module "distribution_probes" {
  source               = "../aws/modules/synthetics_distribution"
  service              = "distribution"
  base_domain          = var.base_domain
  region               = var.canary_deploy_region
  lambda_exec_role_arn = module.lambda_execution_role.role.arn
  logs_bucket_id       = var.logs_bucket_id
  dependency_ref       = module.upload_probes.probe_virology_upload_function_name # create canaries one at a time to avoid 429 errors
  tags                 = var.tags
}
