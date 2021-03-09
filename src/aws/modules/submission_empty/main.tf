locals {
  identifier_prefix = "${terraform.workspace}-${var.name}-sub"
}

resource "aws_cloudfront_origin_access_identity" "this" {
  comment = "Origin access ID for the distribution service in ${terraform.workspace}"
}

module "empty_submission_store" {
  source                      = "../../libraries/distribution_s3"
  name                        = var.name
  service                     = "submission"
  origin_access_identity_path = aws_cloudfront_origin_access_identity.this.iam_arn
  logs_bucket_id              = var.logs_bucket_id
  force_destroy_s3_buckets    = var.force_destroy_s3_buckets
  s3_versioning               = false
  tags                        = var.tags
  policy_document             = var.policy_document
}


resource "aws_s3_bucket_object" "empty_file" {
  bucket       = module.empty_submission_store.bucket.bucket
  key          = "submission/empty-submission-v2"
  source       = abspath("../../../static/empty")
  etag         = filemd5("../../../static/empty")
  content_type = "text/plain"
}
