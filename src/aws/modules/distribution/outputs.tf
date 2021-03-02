output "name" {
  value = var.name
}

output "store" {
  value = module.distribution_store.bucket
}

output "origin_access_identity_path" {
  value = aws_cloudfront_origin_access_identity.this.cloudfront_access_identity_path
}

output "origin_access_identity_arn" {
  value = aws_cloudfront_origin_access_identity.this.iam_arn
}
