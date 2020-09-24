locals {

  metric_period = 1200 # 20 minutes

  metric_alarms = {
    "submission_probes" = {
      desc = "Submission API failures"
      metric_query = [
        {
          id                   = "m1",
          canary_function_name = module.submission_probes.probe_analytics_function_name
        },
        {
          id                   = "m2",
          canary_function_name = module.submission_probes.probe_diag_keys_function_name
        },
        {
          id                   = "m3",
          canary_function_name = module.submission_probes.probe_exp_notif_circ_brkr_function_name
        },
        {
          id                   = "m4",
          canary_function_name = module.submission_probes.probe_rsky_vnue_circ_brkr_function_name
        },
        {
          id                   = "m5",
          canary_function_name = module.submission_probes.probe_virology_test_function_name
        },
      ]
    },

    "upload_probes" = {
      desc = "Upload API failures"
      metric_query = [
        {
          id                   = "m1",
          canary_function_name = module.upload_probes.probe_risky_post_districts_upload_function_name
        },
        {
          id                   = "m2",
          canary_function_name = module.upload_probes.probe_risky_venues_upload_function_name
        },
        {
          id                   = "m3",
          canary_function_name = module.upload_probes.probe_virology_upload_function_name
        }
      ]
    },
    "distribution_probes" = {
      desc = "Distribution API failures"
      metric_query = [
        {
          id                   = "m1",
          canary_function_name = module.distribution_probes.probe_availability_android_distribution_function_name
        },
        {
          id                   = "m2",
          canary_function_name = module.distribution_probes.probe_availability_ios_distribution_function_name
        },
        {
          id                   = "m3",
          canary_function_name = module.distribution_probes.probe_diagnosis_keys_daily_distribution_function_name
        },
        {
          id                   = "m4",
          canary_function_name = module.distribution_probes.probe_exposure_configuration_distribution_function_name
        },
        {
          id                   = "m5",
          canary_function_name = module.distribution_probes.probe_risky_post_district_distribution_function_name
        },
        {
          id                   = "m6",
          canary_function_name = module.distribution_probes.probe_risky_venues_distribution_function_name
        },
        {
          id                   = "m7",
          canary_function_name = module.distribution_probes.probe_self_isolation_distribution_function_name
        },
        {
          id                   = "m8",
          canary_function_name = module.distribution_probes.probe_diagnosis_keys_2hourly_distribution_function_name
        },
        {
          id                   = "m9",
          canary_function_name = module.distribution_probes.probe_symptomatic_questionnaire_distribution_function_name
        },
      ]
    }
  }
}

resource "aws_cloudwatch_metric_alarm" "this" {
  for_each            = local.metric_alarms
  provider            = aws.synth
  alarm_name          = "${terraform.workspace}-${each.key}-failed-alarm"
  alarm_description   = each.value.desc
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  threshold           = "2"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [var.app_alarms_topic_arn]

  metric_query {
    id          = "e1"
    expression  = "SUM(METRICS())"
    label       = "Failures"
    return_data = "true"
  }

  dynamic "metric_query" {
    for_each = toset(lookup(each.value, "metric_query", []))

    content {
      id = lookup(metric_query.value, "id", null)

      metric {
        namespace   = "CloudWatchSynthetics"
        metric_name = "Failed"
        dimensions = {
          CanaryName = lookup(metric_query.value, "canary_function_name", null)
        }

        period = local.metric_period
        stat   = "Sum"
        unit   = "Count"
      }
    }
  }
}
