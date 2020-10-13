# https://docs.aws.amazon.com/waf/latest/developerguide/monitoring-cloudwatch.html#set-ddos-alarms
# This will have to be refined later to quickly identify which public endpoints are being attacked
resource "aws_cloudwatch_dashboard" "ddos_dashboard" {
  dashboard_name = "${terraform.workspace}-DDoS-Monitoring"

  dashboard_body = <<-EOF
  {
    "widgets": [
        {
            "type": "alarm",
            "x": 0,
            "y": 0,
            "width": 24,
            "height": 3,
            "properties": {
                "title": "Alarms",
                "alarms": [
                   
                ]
            }
        },
        {
            "type": "metric",
            "width": 24,
            "height": 6,
            "properties": {
                "metrics": [
                    [ "AWS/DDoSProtection", "DDoSAttackRequestsPerSecond" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "region": "${local.us_east_1}",
                "title": "Layer 7 Attacks",
                "stat": "Sum",
                "period": ${local.metric_period},
                "yAxis": {
                    "left": {
                        "showUnits": false,
                        "label": "Count"
                    },
                    "right": {
                        "label": ""
                    }
                }
            }
        },
        {
            "type": "metric",
            "width": 24,
            "height": 6,
            "properties": {
                "metrics": [
                    [ "AWS/DDoSProtection", "DDoSAttackPacketsPerSecond" ],
                    [".", "DDoSAttackBitsPerSecond", { "yAxis": "right" }]
                ],
                "view": "timeSeries",
                "stacked": false,
                "region": "${local.us_east_1}",
                "title": "Layer 3/4 Attacks",
                "stat": "Sum",
                "period": ${local.metric_period},
                "yAxis": {
                    "left": {
                        "showUnits": false,
                        "label": "Packets"
                    },
                    "right": {
                        "showUnits": false,
                        "label": "Bits"
                    }
                }
            }
        }
    ]
  }
  EOF
}