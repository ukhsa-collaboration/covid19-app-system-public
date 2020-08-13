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
module "probe_risky_venues_upload" {
  source                = "../../libraries/synthetics"
  name                  = "rven-upldp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "upload-${terraform.workspace}"
  uri_path              = "/upload/identified-risk-venues"
  method                = "POST"
  auth_header           = local.test_config.auth_headers.highRiskVenuesCodeUpload
  expc_status           = "422"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
}

module "probe_risky_post_districts_upload" {
  source                = "../../libraries/synthetics"
  name                  = "rpos-upldp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "upload-${terraform.workspace}"
  uri_path              = "/upload/high-risk-postal-districts"
  method                = "POST"
  auth_header           = local.test_config.auth_headers.highRiskPostCodeUpload
  expc_status           = "422"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
}

module "probe_virology_upload" {
  source                = "../../libraries/synthetics"
  name                  = "viro-upldp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "upload-${terraform.workspace}"
  uri_path              = "/upload/virology-test/npex-result"
  method                = "POST"
  auth_header           = local.test_config.auth_headers.testResultUpload
  expc_status           = "422"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
}
