
module "artifact_repository" {
  source = "./libraries/repository_s3"
}

output "lambda_object_key" {
  value = module.artifact_repository.lambda_object_key
}

output "bucket_name" {
  value = module.artifact_repository.bucket_name
}
