module "risky_venues_messages_distribution" {
  source                   = "./modules/distribution"
  name                     = "risky-venue-messages-configuration"
  default_payload          = "download/risky-venue-messages-configuration"
  payload_source           = abspath("../../../static/risky-venues-messages.json")
  metadata_signature       = abspath("../../../../out/signatures/risky-venues-messages.json.sig")
  metadata_signature_date  = abspath("../../../../out/signatures/risky-venues-messages.json.date")
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  s3_versioning            = false
}


module "download_apis" {
  source = "./libraries/cloudfront_download_facade"

  name = "download"

  risky_venues_messages_bucket_regional_domain_name = module.risky_venues_messages_distribution.store.bucket_regional_domain_name
  risky_venues_messages_payload                     = module.risky_venues_messages_distribution.name
  risky_venues_messages_origin_access_identity_path = module.risky_venues_messages_distribution.origin_access_identity_path

  domain      = var.base_domain
  web_acl_arn = data.aws_wafv2_web_acl.this.arn
}

output "base_download_endpoint" {
  value = "https://${module.download_apis.distribution_domain_name}/download"
}

output "risky_venues_messages_download_endpoint" {
  value = "https://${module.download_apis.distribution_domain_name}/download/${module.risky_venues_messages_distribution.name}"
}