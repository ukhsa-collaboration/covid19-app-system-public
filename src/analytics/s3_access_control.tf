module "artifact_repository_access" {
  source        = "../aws/modules/s3_access_policies"
  policy_type   = "default"
  s3_bucket_arn = module.artifact_repository.bucket_arn
}
