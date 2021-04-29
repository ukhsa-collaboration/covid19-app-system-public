locals {
  service = "quicksight"
}

module "archive_store" {
  source                   = "./libraries/analytics_s3"
  name                     = "analytics-archive-quicksight"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

module "output_store" {
  source                   = "./libraries/analytics_s3"
  name                     = "analytics-output-quicksight"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

module "workgroup" {
  source              = "./libraries/athena_workgroup"
  name                = "analytics_quicksight"
  athena_output_store = module.output_store.bucket_name
  tags                = var.tags
}

resource "aws_glue_catalog_database" "this" {
  name = "${terraform.workspace}_analytics_db"
}

module "mobile_analytics" {
  source                                                    = "./modules/mobile_analytics"
  database_name                                             = aws_glue_catalog_database.this.name
  workgroup_name                                            = module.workgroup.name
  analytics_submission_store_parquet_bucket_id              = var.analytics_submission_store_parquet_bucket_id
  analytics_submission_store_consolidated_parquet_bucket_id = var.analytics_submission_store_consolidated_parquet_bucket_id
}

module "app_store_qr_posters" {
  source                   = "./modules/app_store_qr_posters"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  workgroup_name           = module.workgroup.name
  tags                     = var.tags
}

module "risky_post_districts" {
  source                     = "./modules/risky_post_districts"
  database_name              = aws_glue_catalog_database.this.name
  workgroup_name             = module.workgroup.name
  risky_post_codes_bucket_id = var.risky_post_codes_bucket_id
}

module "postcodes_geofence" {
  source                   = "./modules/postcodes_geofence"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  workgroup_name           = module.workgroup.name
  tags                     = var.tags
}

module "demographics_data" {
  source                   = "./modules/local_authorities_demographic_geofence_lookup"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "postcode_demographic_geographic_lookup" {
  source                   = "./modules/postcode_demographic_geographic_lookup"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "full_postcode_lookup" {
  source                   = "./modules/full_postcode_lookup"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "sip_analytics" {
  source         = "./modules/sip_analytics"
  database_name  = aws_glue_catalog_database.this.name
  location       = var.sip_analytics_bucket_location
  workgroup_name = module.workgroup.name
}

module "log_insights_analytics" {

  source                                   = "./modules/log_insights_analytics"
  circuit_breaker_stats_bucket_id          = var.circuit_breaker_stats_bucket_id
  key_federation_download_stats_bucket_id  = var.key_federation_download_stats_bucket_id
  key_federation_upload_stats_bucket_id    = var.key_federation_upload_stats_bucket_id
  diagnosis_key_submission_stats_bucket_id = var.diagnosis_key_submission_stats_bucket_id
  database_name                            = aws_glue_catalog_database.this.name
  logs_bucket_id                           = var.logs_bucket_id
  service                                  = local.service
  tags                                     = var.tags
  workgroup_name                           = module.workgroup.name
}

module "postcode_demographic_geographic_lookup_v2" {
  source                   = "./modules/postcode_demographic_geographic_lookup_v2"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}
