module "analytics" {
  source                                       = "../.."
  force_destroy_s3_buckets                     = true
  logs_bucket_id                               = "dev-s3-logs20200818142711233900000002"
  risky_post_codes_bucket_id                   = "${terraform.workspace}-risky-post-districts-distribution"
  analytics_submission_store_parquet_bucket_id = "${terraform.workspace}-analytics-submission-parquet"
}