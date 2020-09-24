module "agapi-syn" {
  source               = "../../"
  base_domain          = "dev.svc-test-trace.nhs.uk"
  burst_limit          = 5000
  rate_limit           = 10000
  logs_bucket_id       = "dev-s3-logs20200818142711233900000002"
  app_alarms_topic_arn = data.aws_sns_topic.app_alarms_topic.arn
  canary_deploy_region = local.region

  providers = {
    aws   = aws
    synth = aws.synth
  }
}

data "aws_sns_topic" "app_alarms_topic" {
  provider = aws.synth
  name     = "dev-alarm-synthetics"
}
