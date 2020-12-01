
module "artifact_repository" {
  source                   = "./libraries/repository_s3"
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  tags                     = var.tags
}

output "lambda_object_key" {
  value = module.artifact_repository.lambda_object_key
}

output "bucket_name" {
  value = module.artifact_repository.bucket_name
}
