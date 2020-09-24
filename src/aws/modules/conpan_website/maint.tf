resource "aws_cloudfront_origin_access_identity" "this" {
  comment = "Origin access ID for the Control Panel in ${terraform.workspace}"
}

module "conpan_website_s3" {
  source                      = "../../libraries/conpan_s3"
  name                        = var.name
  service                     = "website"
  logs_bucket_id              = var.logs_bucket_id
  origin_access_identity_path = aws_cloudfront_origin_access_identity.this.iam_arn
  force_destroy_s3_buckets    = var.force_destroy_s3_buckets
}

module "distribution_conpan" {
  source = "../../libraries/cloudfront_conpan_facade"

  name                        = "conpan"
  domain                      = var.base_domain
  web_acl_arn                 = var.aws_wafv2_web_acl_arn
  bucket_regional_domain_name = module.conpan_website_s3.bucket.bucket_regional_domain_name
  conpan_path                 = "/*"
  origin_access_identity_path = aws_cloudfront_origin_access_identity.this.cloudfront_access_identity_path
  enable_shield_protection    = var.enable_shield_protection
}
