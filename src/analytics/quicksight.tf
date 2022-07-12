locals {
  service = "quicksight"
}

module "archive_store" {
  source                   = "./libraries/analytics_s3"
  name                     = "analytics-archive-quicksight"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_versioning        = false
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

module "output_store" {
  source                   = "./libraries/analytics_s3"
  name                     = "analytics-output-quicksight"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_versioning        = false
  logs_bucket_id           = var.logs_bucket_id
  lifecycle_rules = [{
    id      = "expire-all-after-7-days"
    prefix  = null
    enabled = true
    days    = 7
  }]
  tags = var.tags
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

module "mobile_events_analytics" {
  source                                        = "./modules/mobile_events_analytics"
  analytics_submission_events_bucket_id         = var.analytics_submission_events_bucket_id
  analytics_submission_events_parquet_bucket_id = var.analytics_submission_events_parquet_bucket_id
  glue_db_name                                  = aws_glue_catalog_database.this.name
  glue_table_name                               = "${terraform.workspace}_analytics_mobile_events"
  workgroup_name                                = module.workgroup.name
}

module "app_store_qr_posters" {
  source                     = "./modules/app_store_qr_posters"
  service                    = local.service
  force_destroy_s3_buckets   = var.force_destroy_s3_buckets
  logs_bucket_id             = var.logs_bucket_id
  database_name              = aws_glue_catalog_database.this.name
  workgroup_name             = module.workgroup.name
  tags                       = var.tags
  qr_posters_bucket_location = var.qr_posters_bucket_location
}

module "risky_post_districts" {
  source                     = "./modules/risky_post_districts"
  database_name              = aws_glue_catalog_database.this.name
  workgroup_name             = module.workgroup.name
  risky_post_codes_bucket_id = var.risky_post_codes_bucket_id
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
  virology_test_stats_bucket_id            = var.virology_test_stats_bucket_id
}

module "postcode_demographic_geographic_lookup_v2" {
  source                   = "./modules/postcode_demographic_geographic_lookup_v2"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "coronavirus_gov_public_data_lookup_england" {
  source                   = "./modules/coronavirus_gov_public_data_lookup"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
  country                  = "england"
}

module "coronavirus_gov_public_data_lookup_wales" {
  source                   = "./modules/coronavirus_gov_public_data_lookup"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
  country                  = "wales"
}

module "analytics_app_store_extended" {
  source                   = "./modules/analytics_app_store_extended"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "analytics_google_installs_overview_report" {
  source                   = "./modules/google_installs_overview_report"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "analytics_apple_sales_report" {
  source                   = "./modules/apple_sales_report"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "analytics_vaccinations_england" {
  source                   = "./modules/analytics_vaccinations_england"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "local_authorities_religion_lookup" {
  source                   = "./modules/local_authorities_religion_lookup"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "local_authorities_ethnicity_lookup" {
  source                   = "./modules/local_authorities_ethnicity_lookup"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "england_deprivation_lookup" {
  source                   = "./modules/england_deprivation_lookup_table"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "risky_venues" {
  source                   = "./modules/risky_venues"
  database_name            = aws_glue_catalog_database.this.name
  workgroup_name           = module.workgroup.name
  risky_venues_bucket_name = var.risky_venues_bucket_name
}

module "ios_historical_ratings" {
  source                   = "./modules/ios_historical_ratings"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "android_historical_ratings" {
  source                   = "./modules/android_historical_ratings"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "quicksight_users" {
  source                   = "./modules/quicksight_users"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "quicksight_usage" {
  source                   = "./modules/quicksight_usage"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "quicksight_data_sets" {
  source                   = "./modules/quicksight_data_sets"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "quicksight_ingestions" {
  source                   = "./modules/quicksight_ingestions"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "quicksight_dashboards" {
  source                   = "./modules/quicksight_dashboards"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "quicksight_dashboards_for_data_sets" {
  source                   = "./modules/quicksight_dashboards_for_data_sets"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "quicksight_data_sources" {
  source                   = "./modules/quicksight_data_sources"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "quicksight_analyses" {
  source                   = "./modules/quicksight_analyses"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "quicksight_analyses_for_data_sets" {
  source                   = "./modules/quicksight_analyses_for_data_sets"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}

module "quicksight_users_for_data_sets" {
  source                   = "./modules/quicksight_users_for_data_sets"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  tags                     = var.tags
}
