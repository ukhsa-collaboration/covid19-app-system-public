locals {
  metric_period = 180 # 3 minutes

  metric_alarms = [{
    name           = "${terraform.workspace}-P3-DDoSDetected",
    description    = "DDoS attack detected by AWS Shield Advanced.  Open up AWS Shield console to see which resources are affected and details about the attack. Refer to the runbook RBK002-ddos (https://github.com/nihp-public/COVID19-app-system/blob/master/doc/ops/run_books/RBK002-ddos.adoc).",
    metric_name    = "DDoSDetected"
    metric_queries = [for arn in var.shield_protected_arns : { "arn" = arn, "id" = "m${index(var.shield_protected_arns, arn)}" }]
    }, {
    name           = "${terraform.workspace}-P3-Layer3_4_DDoSDetected",
    description    = "Layer 3/4 DDoS attack detected by AWS Shield Advanced.  Open up AWS Shield console to see which resources are affected and details about the attack. Refer to the runbook RBK002-ddos (https://github.com/nihp-public/COVID19-app-system/blob/master/doc/ops/run_books/RBK002-ddos.adoc).",
    metric_name    = "DDoSAttackPacketsPerSecond"
    metric_queries = [for arn in var.shield_protected_arns : { "arn" = arn, "id" = "m${index(var.shield_protected_arns, arn)}" }]
    }, {
    name           = "${terraform.workspace}-P1-Layer7_DDoSDetected",
    description    = "Layer 7 DDoS attack detected by AWS Shield Advanced.  Open up AWS Shield console to see which resources are affected and details about the attack. Refer to the runbook RBK002-ddos (https://github.com/nihp-public/COVID19-app-system/blob/master/doc/ops/run_books/RBK002-ddos.adoc)."
    metric_name    = "DDoSAttackRequestsPerSecond"
    metric_queries = [for arn in var.shield_protected_arns : { "arn" = arn, "id" = "m${index(var.shield_protected_arns, arn)}" }]
  }]
}

resource "aws_cloudwatch_metric_alarm" "global" {
  count    = length(local.metric_alarms)
  provider = aws.useast

  tags = var.tags

  alarm_name          = local.metric_alarms[count.index].name
  alarm_description   = local.metric_alarms[count.index].description
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  threshold           = "1"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [var.shield_alarm_set_topic_arn]
  ok_actions          = [var.shield_alarm_ok_topic_arn]

  metric_query {
    id          = "e1"
    expression  = "SUM(METRICS())"
    label       = local.metric_alarms[count.index].metric_name
    return_data = "true"
  }

  dynamic "metric_query" {
    for_each = toset(local.metric_alarms[count.index].metric_queries)

    content {
      id = lookup(metric_query.value, "id", null)

      metric {
        namespace   = "AWS/DDoSProtection"
        metric_name = local.metric_alarms[count.index].metric_name
        dimensions = {
          ResourceArn = lookup(metric_query.value, "arn", null)
        }

        period = local.metric_period
        stat   = "Sum"
        unit   = "Count"
      }
    }
  }
}
