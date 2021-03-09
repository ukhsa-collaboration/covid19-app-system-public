output "store" {
  value = module.empty_submission_store
}

output "store_id" {
  value = module.empty_submission_store.bucket_id
}

output "store_arn" {
  value = module.empty_submission_store.bucket.arn
}

output "store_regional_domain_name" {
  value = module.empty_submission_store.bucket.bucket_regional_domain_name
}

output "origin_access_identity_arn" {
  value = aws_cloudfront_origin_access_identity.this.iam_arn
}

output "origin_access_identity_path" {
  value = aws_cloudfront_origin_access_identity.this.cloudfront_access_identity_path
}
