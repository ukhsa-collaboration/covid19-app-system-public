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
module "probe_analytics" {
  source                = "../../libraries/synthetics"
  name                  = "analyticsp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "submission-${terraform.workspace}"
  uri_path              = "/submission/mobile-analytics"
  method                = "POST"
  auth_header           = local.test_config.auth_headers.mobile
  expc_status           = "400"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
}

module "probe_diag_keys" {
  source                = "../../libraries/synthetics"
  name                  = "diagkeysp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "submission-${terraform.workspace}"
  uri_path              = "/submission/diagnosis-keys"
  method                = "POST"
  auth_header           = local.test_config.auth_headers.mobile
  expc_status           = "400"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
}

module "probe_exp_notif_circ_brkr" {
  source                = "../../libraries/synthetics"
  name                  = "expnot-cbp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "submission-${terraform.workspace}"
  uri_path              = "/circuit-breaker/exposure-notification/request"
  method                = "POST"
  auth_header           = local.test_config.auth_headers.mobile
  expc_status           = "422"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
}

module "probe_rsky_vnue_circ_brkr" {
  source                = "../../libraries/synthetics"
  name                  = "rskven-cbp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "submission-${terraform.workspace}"
  uri_path              = "/circuit-breaker/venue/request"
  method                = "POST"
  auth_header           = local.test_config.auth_headers.mobile
  expc_status           = "422"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
}

module "probe_virology_test" {
  source                = "../../libraries/synthetics"
  name                  = "virologytp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "submission-${terraform.workspace}"
  uri_path              = "/virology-test/home-kit/order"
  method                = "POST"
  auth_header           = local.test_config.auth_headers.mobile
  expc_status           = "200"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
}
