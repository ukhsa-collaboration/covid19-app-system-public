module "post_districts_distribution_analytics_access" {
  source                 = "./modules/cross_account_s3_readonly_policy"
  prefix                 = "analytics"
  principal_aws_accounts = var.analytics_aws_accounts
  s3_bucket_arn          = module.post_districts_distribution.store.arn
}

module "analytics_submission_store_parquet_analytics_access" {
  source                 = "./modules/cross_account_s3_readonly_policy"
  prefix                 = "analytics"
  principal_aws_accounts = var.analytics_aws_accounts
  s3_bucket_arn          = module.analytics_submission_store_parquet.bucket_arn
}
