module "analytics" {
  source                                       = "../.."
  force_destroy_s3_buckets                     = true
  logs_bucket_id                               = "staging-s3-logs20200803210344955400000001"
  risky_post_codes_bucket_id                   = "te-staging-risky-post-districts-distribution"
  analytics_submission_store_parquet_bucket_id = "te-staging-analytics-submission-parquet"
}
