locals {
  workspace_te = regex("^(?P<prefix>te-)?(?P<target>[^-]+)$", terraform.workspace)
  workspace_id = (local.workspace_te.prefix == null) ? "branch" : local.workspace_te.target
  test_config  = jsondecode(file("${path.root}/../../../../out/gen/config/test_config_${local.workspace_id}.json"))
}

data "aws_caller_identity" "current" {}

module "lambda_storage" {
  source  = "../../libraries/non_logging_s3"
  name    = "probe"
  service = var.service
}

# maximum name length including workspace prefix is 21.
# maximum prefix length is currently "te-staging-" i.e. we have only 10 characters to play with.
module "probe_exposure_configuration_distribution" {
  source                = "../../libraries/synthetics"
  name                  = "expco-dstp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "distribution-${terraform.workspace}"
  uri_path              = "/distribution/exposure-configuration"
  method                = "GET"
  auth_header           = ""
  expc_status           = "200"
  service               = var.service
  api_gw_support        = false
  lambda_exec_role_arn  = var.lambda_exec_role_arn
}

module "probe_risky_post_district_distribution" {
  source                = "../../libraries/synthetics"
  name                  = "rskpd-dstp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "distribution-${terraform.workspace}"
  uri_path              = "/distribution/risky-post-districts"
  method                = "GET"
  auth_header           = ""
  expc_status           = "200"
  service               = var.service
  api_gw_support        = false
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_exposure_configuration_distribution.function_name # create canaries one at a time to avoid 429 errors
}

module "probe_risky_venues_distribution" {
  source                = "../../libraries/synthetics"
  name                  = "rskve-dstp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "distribution-${terraform.workspace}"
  uri_path              = "/distribution/risky-venues"
  method                = "GET"
  auth_header           = ""
  expc_status           = "200"
  service               = var.service
  api_gw_support        = false
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_risky_post_district_distribution.function_name # create canaries one at a time to avoid 429 errors
}

module "probe_self_isolation_distribution" {
  source                = "../../libraries/synthetics"
  name                  = "slfis-dstp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "distribution-${terraform.workspace}"
  uri_path              = "/distribution/self-isolation"
  method                = "GET"
  auth_header           = ""
  expc_status           = "200"
  service               = var.service
  api_gw_support        = false
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_risky_venues_distribution.function_name # create canaries one at a time to avoid 429 errors
}

module "probe_symptomatic_questionnaire_distribution" {
  source                = "../../libraries/synthetics"
  name                  = "sympq-dstp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "distribution-${terraform.workspace}"
  uri_path              = "/distribution/symptomatic-questionnaire"
  method                = "GET"
  auth_header           = ""
  expc_status           = "200"
  service               = var.service
  api_gw_support        = false
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_self_isolation_distribution.function_name # create canaries one at a time to avoid 429 errors
}

module "probe_availability_android_distribution" {
  source                = "../../libraries/synthetics"
  name                  = "avand-dstp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "distribution-${terraform.workspace}"
  uri_path              = "/distribution/availability-android"
  method                = "GET"
  auth_header           = ""
  expc_status           = "200"
  service               = var.service
  api_gw_support        = false
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_symptomatic_questionnaire_distribution.function_name # create canaries one at a time to avoid 429 errors
}

module "probe_availability_ios_distribution" {
  source                = "../../libraries/synthetics"
  name                  = "avios-dstp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "distribution-${terraform.workspace}"
  uri_path              = "/distribution/availability-ios"
  method                = "GET"
  auth_header           = ""
  expc_status           = "200"
  service               = var.service
  api_gw_support        = false
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_availability_android_distribution.function_name # create canaries one at a time to avoid 429 errors
}

module "probe_diagnosis_keys_daily_distribution" {
  source                = "../../libraries/synthetics"
  name                  = "daily-dstp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-keys-distribution.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "distribution-${terraform.workspace}"
  uri_path              = "/distribution/daily"
  method                = "GET"
  auth_header           = local.test_config.auth_headers.mobile
  expc_status           = "200"
  service               = var.service
  api_gw_support        = false
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_availability_ios_distribution.function_name # create canaries one at a time to avoid 429 errors
}

module "probe_diagnosis_keys_2hourly_distribution" {
  source                = "../../libraries/synthetics"
  name                  = "2hrly-dstp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-keys-distribution.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "distribution-${terraform.workspace}"
  uri_path              = "/distribution/two-hourly"
  method                = "GET"
  auth_header           = local.test_config.auth_headers.mobile
  expc_status           = "200"
  service               = var.service
  api_gw_support        = false
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_diagnosis_keys_daily_distribution.function_name # create canaries one at a time to avoid 429 errors
}
