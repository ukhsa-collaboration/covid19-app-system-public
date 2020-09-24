module "analytics" {
  source                                       = "../.."
  force_destroy_s3_buckets                     = true
  logs_bucket_id                               = "prod-s3-logs20200801203654277400000001"
  risky_post_codes_bucket_id                   = "te-prod-risky-post-districts-distribution"
  analytics_submission_store_parquet_bucket_id = "te-prod-analytics-submission-parquet"
}
