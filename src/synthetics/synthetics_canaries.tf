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

output "probe_analytics_function_name" {
  value = module.submission_probes.probe_analytics_function_name
}

output "probe_diag_keys_function_name" {
  value = module.submission_probes.probe_diag_keys_function_name
}

output "probe_exp_notif_circ_brkr_function_name" {
  value = module.submission_probes.probe_exp_notif_circ_brkr_function_name
}

output "probe_rsky_vnue_circ_brkr_function_name" {
  value = module.submission_probes.probe_rsky_vnue_circ_brkr_function_name
}

output "probe_virology_test_function_name" {
  value = module.submission_probes.probe_virology_test_function_name
}

output "probe_risky_post_districts_upload_function_name" {
  value = module.upload_probes.probe_risky_post_districts_upload_function_name
}

output "probe_risky_venues_upload_function_name" {
  value = module.upload_probes.probe_risky_venues_upload_function_name
}

output "probe_virology_upload_function_name" {
  value = module.upload_probes.probe_virology_upload_function_name
}

output "probe_availability_android_distribution_function_name" {
  value = module.distribution_probes.probe_availability_android_distribution_function_name
}

output "probe_availability_ios_distribution_function_name" {
  value = module.distribution_probes.probe_availability_ios_distribution_function_name
}

output "probe_diagnosis_keys_2hourly_distribution_function_name" {
  value = module.distribution_probes.probe_diagnosis_keys_2hourly_distribution_function_name
}

output "probe_diagnosis_keys_daily_distribution_function_name" {
  value = module.distribution_probes.probe_diagnosis_keys_daily_distribution_function_name
}

output "probe_exposure_configuration_distribution_function_name" {
  value = module.distribution_probes.probe_exposure_configuration_distribution_function_name
}

output "probe_risky_post_district_distribution_function_name" {
  value = module.distribution_probes.probe_risky_post_district_distribution_function_name
}

output "probe_risky_venues_distribution_function_name" {
  value = module.distribution_probes.probe_risky_venues_distribution_function_name
}

output "probe_self_isolation_distribution_function_name" {
  value = module.distribution_probes.probe_self_isolation_distribution_function_name
}

output "probe_symptomatic_questionnaire_distribution_function_name" {
  value = module.distribution_probes.probe_symptomatic_questionnaire_distribution_function_name
}
