
module "artifact_repository" {
  source                   = "./libraries/repository_s3"
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  policy_document          = module.artifact_repository_access.policy_document
  tags                     = var.tags
  lambda_project           = "cta-services"
  lambda_zip_path          = "../../../../out/build/javalambda-0.0.1-SNAPSHOT.zip"
}

output "lambda_object_key" {
  value = module.artifact_repository.lambda_object_key
}

output "bucket_name" {
  value = module.artifact_repository.bucket_name
}
