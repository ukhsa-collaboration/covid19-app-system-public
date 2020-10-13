module "lambda_storage" {
  source                   = "../../libraries/submission_s3"
  name                     = "probe"
  service                  = var.service
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = true
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
  uri_path              = "/submission/mobile-analytics/health"
  method                = "POST"
  secret_name           = "/mobile/synthetic_canary_auth"
  expc_status           = "200"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = var.dependency_ref # create canaries one at a time to avoid 429 errors
}

module "probe_diag_keys" {
  source                = "../../libraries/synthetics"
  name                  = "diagkeysp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "submission-${terraform.workspace}"
  uri_path              = "/submission/diagnosis-keys/health"
  method                = "POST"
  secret_name           = "/mobile/synthetic_canary_auth"
  expc_status           = "200"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_analytics.function_name # create canaries one at a time to avoid 429 errors
}

module "probe_exp_notif_circ_brkr" {
  source                = "../../libraries/synthetics"
  name                  = "expnot-cbp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "submission-${terraform.workspace}"
  uri_path              = "/circuit-breaker/exposure-notification/health"
  method                = "POST"
  secret_name           = "/mobile/synthetic_canary_auth"
  expc_status           = "200"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_diag_keys.function_name # create canaries one at a time to avoid 429 errors
}

module "probe_rsky_vnue_circ_brkr" {
  source                = "../../libraries/synthetics"
  name                  = "rskven-cbp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "submission-${terraform.workspace}"
  uri_path              = "/circuit-breaker/venue/health"
  method                = "POST"
  secret_name           = "/mobile/synthetic_canary_auth"
  expc_status           = "200"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_exp_notif_circ_brkr.function_name # create canaries one at a time to avoid 429 errors
}

module "probe_virology_test" {
  source                = "../../libraries/synthetics"
  name                  = "virologytp"
  synthetic_script_path = "${path.module}/../../lambdas/synthetics/synthetics-canary.js"
  artifact_s3_bucket    = module.lambda_storage.bucket_name
  base_domain           = var.base_domain
  hostname              = "submission-${terraform.workspace}"
  uri_path              = "/virology-test/health"
  method                = "POST"
  secret_name           = "/mobile/synthetic_canary_auth"
  expc_status           = "200"
  service               = var.service
  api_gw_support        = true
  lambda_exec_role_arn  = var.lambda_exec_role_arn
  dependency_ref        = module.probe_rsky_vnue_circ_brkr.function_name # create canaries one at a time to avoid 429 errors
}
