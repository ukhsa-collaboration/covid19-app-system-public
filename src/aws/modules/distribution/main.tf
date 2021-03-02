resource "aws_cloudfront_origin_access_identity" "this" {
  comment = "Origin access ID for the distribution service in ${terraform.workspace}"
}

module "distribution_store" {
  source                      = "../../libraries/distribution_s3"
  name                        = var.name
  service                     = "distribution"
  origin_access_identity_path = aws_cloudfront_origin_access_identity.this.iam_arn
  logs_bucket_id              = var.logs_bucket_id
  force_destroy_s3_buckets    = var.force_destroy_s3_buckets
  s3_versioning               = var.s3_versioning
  tags                        = var.tags
  policy_document             = var.policy_document
}

resource "aws_s3_bucket_object" "payload" {
  count        = var.default_payload == null ? 0 : 1
  bucket       = module.distribution_store.bucket.bucket
  key          = var.default_payload
  source       = var.payload_source
  etag         = filemd5(var.payload_source)
  content_type = "application/json"
  metadata = {
    signature      = file(var.metadata_signature)
    signature-date = file(var.metadata_signature_date)
  }
}

resource "aws_s3_bucket_object" "version" {
  bucket       = module.distribution_store.bucket.bucket
  key          = "version"
  source       = "${path.module}/../../../../out/version.sha"
  etag         = filemd5("${path.module}/../../../../out/version.sha")
  content_type = "text/plain"
}
