locals {
  service = "quicksight"
}

module "output_store" {
  source                   = "./libraries/analytics_s3"
  name                     = "analytics-output-quicksight"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
}

module "workgroup" {
  source              = "./libraries/athena_workgroup"
  name                = "analytics_quicksight"
  athena_output_store = module.output_store.bucket_name
}

resource "aws_glue_catalog_database" "this" {
  name = "${terraform.workspace}_analytics_db"
}

module "mobile_analytics" {
  source                                       = "./modules/mobile_analytics"
  database_name                                = aws_glue_catalog_database.this.name
  workgroup_name                               = module.workgroup.name
  analytics_submission_store_parquet_bucket_id = var.analytics_submission_store_parquet_bucket_id
}

module "app_store_qr_posters" {
  source                   = "./modules/app_store_qr_posters"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
  workgroup_name           = module.workgroup.name
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
}

module "demographics_data" {
  source                   = "./modules/demographic_data"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
}

module "local_authorities_geofence" {
  source                   = "./modules/local_authorities_geofence"
  service                  = local.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  database_name            = aws_glue_catalog_database.this.name
}
