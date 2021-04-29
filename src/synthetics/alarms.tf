locals {

  metric_period = 300 # 5 minutes

  metric_alarms = [
    module.submission_probes.probe_analytics_function_name,
    module.submission_probes.probe_diag_keys_function_name,
    module.submission_probes.probe_exp_notif_circ_brkr_function_name,
    module.submission_probes.probe_rsky_vnue_circ_brkr_function_name,
    module.submission_probes.probe_virology_test_function_name,
    module.upload_probes.probe_risky_post_districts_upload_function_name,
    module.upload_probes.probe_risky_venues_upload_function_name,
    module.upload_probes.probe_virology_upload_function_name,
    module.distribution_probes.probe_availability_android_distribution_function_name,
    module.distribution_probes.probe_availability_ios_distribution_function_name,
    module.distribution_probes.probe_diagnosis_keys_daily_distribution_function_name,
    module.distribution_probes.probe_exposure_configuration_distribution_function_name,
    module.distribution_probes.probe_risky_post_district_distribution_function_name,
    module.distribution_probes.probe_risky_venues_distribution_function_name,
    module.distribution_probes.probe_self_isolation_distribution_function_name,
    module.distribution_probes.probe_diagnosis_keys_2hourly_distribution_function_name,
    module.distribution_probes.probe_symptomatic_questionnaire_distribution_function_name
  ]
}

resource "aws_cloudwatch_metric_alarm" "this" {
  for_each            = toset(local.metric_alarms)
  provider            = aws.synth
  alarm_name          = "${each.value}-failed-alarm"
  alarm_description   = "${each.value} synthetics failed"
  namespace           = "CloudWatchSynthetics"
  statistic           = "Average"
  metric_name         = "SuccessPercent"
  comparison_operator = "LessThanThreshold"
  threshold           = "100"
  period              = local.metric_period
  evaluation_periods  = "4"
  datapoints_to_alarm = "2"
  treat_missing_data  = "missing"
  alarm_actions       = [var.app_alarms_topic_arn]
  dimensions = {
    CanaryName = each.value
  }

  tags = var.tags
}
