module "lambda_storage" {
  source                   = "../../libraries/submission_s3"
  name                     = "probe"
  service                  = var.service
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = true
  tags                     = var.tags
}

# maximum name length including workspace prefix is 21.
# maximum prefix length is currently "te-staging-" i.e. we have only 10 characters to play with.
module "probe_risky_venues_upload" {
  enabled               = var.enabled
  source                = "../../libraries/synthetics"
  name                  = "rven-upldp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "upload-${terraform.workspace}"
  uri_path              = "/upload/identified-risk-venues/health"
  method                = "POST"
  secret_name           = "/highRiskVenuesCodeUpload/synthetic_canary_auth"
  expc_status           = "200"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = var.dependency_ref # create canaries one at a time to avoid 429 errors
}

module "probe_risky_post_districts_upload" {
  enabled               = var.enabled
  source                = "../../libraries/synthetics"
  name                  = "rpos-upldp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "upload-${terraform.workspace}"
  uri_path              = "/upload/high-risk-postal-districts/health"
  method                = "POST"
  secret_name           = "/highRiskPostCodeUpload/synthetic_canary_auth"
  expc_status           = "200"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_risky_venues_upload.function_name # create canaries one at a time to avoid 429 errors
}

module "probe_virology_upload" {
  enabled               = var.enabled
  source                = "../../libraries/synthetics"
  name                  = "viro-upldp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "upload-${terraform.workspace}"
  uri_path              = "/upload/virology-test/health"
  method                = "POST"
  secret_name           = "/testResultUpload/synthetic_canary_auth"
  expc_status           = "200"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_risky_post_districts_upload.function_name # create canaries one at a time to avoid 429 errors
}
