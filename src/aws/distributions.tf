module "exposure_configuration_distribution" {
  source                   = "./modules/distribution"
  name                     = "exposure-configuration"
  default_payload          = "/distribution/exposure-configuration"
  payload_source           = abspath("../../../static/exposure-configuration.json")
  metadata_signature       = abspath("../../../../out/signatures/exposure-configuration.json.sig")
  metadata_signature_date  = abspath("../../../../out/signatures/exposure-configuration.json.date")
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  s3_versioning            = false
  policy_document          = module.exposure_configuration_distribution_access.policy_document
  tags                     = var.tags
}

module "post_districts_distribution" {
  source                   = "./modules/distribution"
  name                     = "risky-post-districts"
  default_payload          = null
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  s3_versioning            = var.s3_versioning
  policy_document          = module.post_districts_distribution_access.policy_document
  tags                     = var.tags
}

module "risky_venues_distribution" {
  source                   = "./modules/distribution"
  name                     = "risky-venues"
  default_payload          = null
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  s3_versioning            = var.s3_versioning
  policy_document          = module.risky_venues_distribution_access.policy_document
  tags                     = var.tags
}

module "self_isolation_distribution" {
  source                   = "./modules/distribution"
  name                     = "self-isolation"
  default_payload          = "/distribution/self-isolation"
  payload_source           = abspath("../../../static/self-isolation.json")
  metadata_signature       = abspath("../../../../out/signatures/self-isolation.json.sig")
  metadata_signature_date  = abspath("../../../../out/signatures/self-isolation.json.date")
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  s3_versioning            = false
  policy_document          = module.self_isolation_distribution_access.policy_document
  tags                     = var.tags
}

module "symptomatic_questionnaire_distribution" {
  source                   = "./modules/distribution"
  name                     = "symptomatic-questionnaire"
  default_payload          = "/distribution/symptomatic-questionnaire"
  payload_source           = abspath("../../../static/symptomatic-questionnaire.json")
  metadata_signature       = abspath("../../../../out/signatures/symptomatic-questionnaire.json.sig")
  metadata_signature_date  = abspath("../../../../out/signatures/symptomatic-questionnaire.json.date")
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  s3_versioning            = false
  policy_document          = module.symptomatic_questionnaire_distribution_access.policy_document
  tags                     = var.tags
}

module "availability_android_distribution" {
  source                   = "./modules/distribution"
  name                     = "availability-android"
  default_payload          = "/distribution/availability-android"
  payload_source           = abspath("../../../static/availability-android.json")
  metadata_signature       = abspath("../../../../out/signatures/availability-android.json.sig")
  metadata_signature_date  = abspath("../../../../out/signatures/availability-android.json.date")
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  s3_versioning            = false
  policy_document          = module.availability_android_distribution_access.policy_document
  tags                     = var.tags
}

module "availability_ios_distribution" {
  source                   = "./modules/distribution"
  name                     = "availability-ios"
  default_payload          = "/distribution/availability-ios"
  payload_source           = abspath("../../../static/availability-ios.json")
  metadata_signature       = abspath("../../../../out/signatures/availability-ios.json.sig")
  metadata_signature_date  = abspath("../../../../out/signatures/availability-ios.json.date")
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  s3_versioning            = false
  policy_document          = module.availability_ios_distribution_access.policy_document
  tags                     = var.tags
}

module "risky_venue_configuration_distribution" {
  source                   = "./modules/distribution"
  name                     = "risky-venue-configuration"
  default_payload          = "/distribution/risky-venue-configuration"
  payload_source           = abspath("../../../static/risky-venue-configuration.json")
  metadata_signature       = abspath("../../../../out/signatures/risky-venue-configuration.json.sig")
  metadata_signature_date  = abspath("../../../../out/signatures/risky-venue-configuration.json.date")
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  s3_versioning            = false
  policy_document          = module.risky_venue_configuration_distribution_access.policy_document
  tags                     = var.tags
}


resource "aws_cloudfront_origin_access_identity" "diagnosis_keys" {
  comment = "Origin access ID for the key distribution service in ${terraform.workspace}"
}

module "diagnosis_keys_distribution_store" {
  source                      = "./libraries/distribution_s3"
  name                        = "diagnosis"
  service                     = "key-distribution"
  origin_access_identity_path = aws_cloudfront_origin_access_identity.diagnosis_keys.iam_arn
  logs_bucket_id              = var.logs_bucket_id
  force_destroy_s3_buckets    = var.force_destroy_s3_buckets
  policy_document             = module.diagnosis_keys_distribution_store_access.policy_document
  tags                        = var.tags
}

module "local_messages_distribution" {
  source                   = "./modules/distribution"
  name                     = "local-messages"
  default_payload          = null
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  s3_versioning            = var.s3_versioning
  policy_document          = module.local_messages_distribution_access.policy_document
  tags                     = var.tags
}

module "distribution_apis" {
  source = "./libraries/cloudfront_distribution_facade"

  name = "distribution"

  exposure_configuration_bucket_regional_domain_name = module.exposure_configuration_distribution.store.bucket_regional_domain_name
  exposure_configuration_payload                     = module.exposure_configuration_distribution.name
  exposure_configuration_origin_access_identity_path = module.exposure_configuration_distribution.origin_access_identity_path

  risky_post_district_distribution_bucket_regional_domain_name = module.post_districts_distribution.store.bucket_regional_domain_name
  risky_post_district_payload                                  = module.post_districts_distribution.name
  risky_post_district_v2_payload                               = "risky-post-districts-v2"
  risky_post_district_origin_access_identity_path              = module.post_districts_distribution.origin_access_identity_path

  risky_venues_bucket_regional_domain_name = module.risky_venues_distribution.store.bucket_regional_domain_name
  risky_venues_payload                     = module.risky_venues_distribution.name
  risky_venues_origin_access_identity_path = module.risky_venues_distribution.origin_access_identity_path

  self_isolation_bucket_regional_domain_name = module.self_isolation_distribution.store.bucket_regional_domain_name
  self_isolation_payload                     = module.self_isolation_distribution.name
  self_isolation_origin_access_identity_path = module.self_isolation_distribution.origin_access_identity_path

  symptomatic_questionnaire_bucket_regional_domain_name = module.symptomatic_questionnaire_distribution.store.bucket_regional_domain_name
  symptomatic_questionnaire_payload                     = module.symptomatic_questionnaire_distribution.name
  symptomatic_questionnaire_origin_access_identity_path = module.symptomatic_questionnaire_distribution.origin_access_identity_path

  diagnosis_keys_bucket_regional_domain_name = module.diagnosis_keys_distribution_store.bucket.bucket_regional_domain_name
  diagnosis_keys_origin_access_identity_path = aws_cloudfront_origin_access_identity.diagnosis_keys.cloudfront_access_identity_path
  diagnosis_keys_path_2hourly                = "/distribution/two-hourly/*"
  diagnosis_keys_path_daily                  = "/distribution/daily/*"

  availability_android_bucket_regional_domain_name = module.availability_android_distribution.store.bucket_regional_domain_name
  availability_android_payload                     = module.availability_android_distribution.name
  availability_android_origin_access_identity_path = module.availability_android_distribution.origin_access_identity_path

  availability_ios_bucket_regional_domain_name = module.availability_ios_distribution.store.bucket_regional_domain_name
  availability_ios_payload                     = module.availability_ios_distribution.name
  availability_ios_origin_access_identity_path = module.availability_ios_distribution.origin_access_identity_path

  risky_venue_configuration_bucket_regional_domain_name = module.risky_venue_configuration_distribution.store.bucket_regional_domain_name
  risky_venue_configuration_payload                     = module.risky_venue_configuration_distribution.name
  risky_venue_configuration_origin_access_identity_path = module.risky_venue_configuration_distribution.origin_access_identity_path

  local_messages_bucket_regional_domain_name = module.local_messages_distribution.store.bucket_regional_domain_name
  local_messages_payload                     = module.local_messages_distribution.name
  local_messages_origin_access_identity_path = module.local_messages_distribution.origin_access_identity_path

  domain                   = var.base_domain
  web_acl_arn              = var.waf_arn
  enable_shield_protection = var.enable_shield_protection
  distribution_cache_ttl   = var.distribution_cache_ttl

  tags = var.tags
}

output "base_distribution_endpoint" {
  value = "https://${module.distribution_apis.distribution_domain_name}/distribution"
}
output "exposure_configuration_distribution_endpoint" {
  value = "https://${module.distribution_apis.distribution_domain_name}/distribution/${module.exposure_configuration_distribution.name}"
}
output "post_districts_distribution_endpoint" {
  value = "https://${module.distribution_apis.distribution_domain_name}/distribution/${module.post_districts_distribution.name}"
}
output "risky_venues_distribution_endpoint" {
  value = "https://${module.distribution_apis.distribution_domain_name}/distribution/${module.risky_venues_distribution.name}"
}
output "self_isolation_distribution_endpoint" {
  value = "https://${module.distribution_apis.distribution_domain_name}/distribution/${module.self_isolation_distribution.name}"
}
output "symptomatic_questionnaire_distribution_endpoint" {
  value = "https://${module.distribution_apis.distribution_domain_name}/distribution/${module.symptomatic_questionnaire_distribution.name}"
}
output "diagnosis_keys_distribution_daily_endpoint" {
  value = "https://${module.distribution_apis.distribution_domain_name}/distribution/daily"
}
output "diagnosis_keys_distribution_2hourly_endpoint" {
  value = "https://${module.distribution_apis.distribution_domain_name}/distribution/two-hourly"
}
output "availability_android_distribution_endpoint" {
  value = "https://${module.distribution_apis.distribution_domain_name}/distribution/${module.availability_android_distribution.name}"
}
output "availability_ios_distribution_endpoint" {
  value = "https://${module.distribution_apis.distribution_domain_name}/distribution/${module.availability_ios_distribution.name}"
}
output "risky_venue_configuration_distribution_endpoint" {
  value = "https://${module.distribution_apis.distribution_domain_name}/distribution/${module.risky_venue_configuration_distribution.name}"
}
output "local_messages_distribution_endpoint" {
  value = "https://${module.distribution_apis.distribution_domain_name}/distribution/${module.local_messages_distribution.name}"
}

output "exposure_configuration_distribution_store" {
  value = module.exposure_configuration_distribution.store.bucket
}
output "post_districts_distribution_store" {
  value = module.post_districts_distribution.store.bucket
}
output "risky_venues_distribution_store" {
  value = module.risky_venues_distribution.store.bucket
}
output "self_isolation_distribution_store" {
  value = module.self_isolation_distribution.store.bucket
}
output "symptomatic_questionnaire_distribution_store" {
  value = module.symptomatic_questionnaire_distribution.store.bucket
}
output "diagnosis_keys_distribution_store" {
  value = module.diagnosis_keys_distribution_store.bucket.bucket
}
output "availability_android_distribution_store" {
  value = module.availability_android_distribution.store.bucket
}
output "availability_ios_distribution_store" {
  value = module.availability_ios_distribution.store.bucket
}
output "risky_venue_configuration_distribution_store" {
  value = module.risky_venue_configuration_distribution.store.bucket
}
output "local_messages_distribution_store" {
  value = module.local_messages_distribution.store.bucket
}
