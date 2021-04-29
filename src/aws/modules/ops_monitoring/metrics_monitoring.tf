locals {
  submission_lambdas = [
    var.analytics_submission_function,
    var.diagnosis_keys_submission_function,
    var.virology_submission_function,
    var.isolation_payment_order_function,
    var.isolation_payment_verify_function,
    var.isolation_payment_consume_function
  ]

  circuit_breaker_lambdas = [
    var.exposure_notification_circuit_breaker_function,
    var.risky_venues_circuit_breaker_function
  ]

  upload_lambdas = [
    var.risky_post_districts_upload_function,
    var.risky_venues_upload_function,
    var.virology_upload_function
  ]

  processing_lambdas = [
    var.diagnosis_keys_processing_function,
  ]

  app_interface_group       = concat(local.submission_lambdas, local.circuit_breaker_lambdas)
  external_interface_group  = local.upload_lambdas
  internal_processing_group = local.processing_lambdas

  app_interface_error_query       = formatlist("SOURCE '/aws/lambda/%s' |", local.app_interface_group)
  external_interface_error_query  = formatlist("SOURCE '/aws/lambda/%s' |", local.external_interface_group)
  internal_processing_error_query = formatlist("SOURCE '/aws/lambda/%s' |", local.internal_processing_group)
}

resource "aws_cloudwatch_dashboard" "lambdas_dashboard" {
  dashboard_name = "${var.env}-Lambdas-dashboard"
  dashboard_body = jsonencode(
    {
      start : "-P1D",
      periodOverride : "auto",
      widgets : [
        {
          type : "metric",
          x : 0,
          y : 0,
          width : 12,
          height : 6,
          properties : {
            metrics : [
              for lambda in local.submission_lambdas : ["AWS/Lambda", "Duration", "FunctionName", lambda, "Resource", lambda]
            ],
            view : "timeSeries",
            stacked : false,
            region : "eu-west-2",
            stat : "Average",
            period : 60,
            title : "Submission lambdas execution duration"
          }
        },
        {
          type : "metric",
          x : 12,
          y : 0,
          width : 12,
          height : 6,
          properties : {
            metrics : [
              for lambda in local.upload_lambdas : ["AWS/Lambda", "Duration", "FunctionName", lambda, "Resource", lambda]
            ],
            view : "timeSeries",
            stacked : false,
            region : "eu-west-2",
            stat : "Average",
            period : 60,
            title : "Upload lambdas execution duration"
          }
        },
        {
          type : "metric",
          x : 0,
          y : 6,
          width : 12,
          height : 6,
          properties : {
            metrics : [
              for lambda in local.processing_lambdas : ["AWS/Lambda", "Duration", "FunctionName", lambda, "Resource", lambda]
            ],
            view : "timeSeries",
            stacked : false,
            region : "eu-west-2",
            stat : "Average",
            period : 60,
            title : "Processing lambdas execution duration"
          }
        },
        {
          type : "metric",
          x : 12,
          y : 6,
          width : 12,
          height : 6,
          properties : {
            metrics : [
              for lambda in local.circuit_breaker_lambdas : ["AWS/Lambda", "Duration", "FunctionName", lambda, "Resource", lambda]
            ],
            view : "timeSeries",
            stacked : false,
            region : "eu-west-2",
            stat : "Average",
            period : 60,
            title : "Circuit-breaker lambdas execution duration"
          }
        }
      ]
    }
  )
}

resource "aws_cloudwatch_dashboard" "errors_dashboard" {
  dashboard_name = "${var.env}-API-errors-warnings-monitoring"
  dashboard_body = <<EOF
  {
   "start": "-P1W",
   "periodOverride": "auto",
   "widgets": [
      {
        "type": "metric",
        "x": 0,
        "y": 0,
        "width": 12,
        "height": 6,
        "properties":
        {
          "metrics": [
            %{for lambda in local.internal_processing_group~}
              [ "ErrorLogCount", "${lambda}-errors", { "id" : "${replace(lambda, "-", "")}_errors" } ],
              [ "WarningLogCount", "${lambda}-warnings", { "id" : "${replace(lambda, "-", "")}_warnings" } ],
            %{endfor~}
            [ { "expression": "SUM(METRICS(\"errors\"))", "id": "e1", "label": "Total number of errors" } ],
            [ { "expression": "SUM(METRICS(\"warnings\"))", "id": "e2", "label": "Total number of warnings" } ]
          ],
          "period": 60,
          "region": "eu-west-2",
          "stacked": false,
          "stat": "Sum",
          "title": "# Errors and Warnings in internal lambdas",
          "view": "timeSeries"
        }
      },
      {
        "type": "metric",
        "x": 12,
        "y": 0,
        "width": 12,
        "height": 6,
        "properties":
        {
          "metrics": [
            %{for lambda in local.external_interface_group~}
              [ "ErrorLogCount", "${lambda}-errors", { "id" : "${replace(lambda, "-", "")}_errors" } ],
              [ "WarningLogCount", "${lambda}-warnings", { "id" : "${replace(lambda, "-", "")}_warnings" } ],
            %{endfor~}
            [ { "expression": "SUM(METRICS(\"errors\"))", "id": "e1", "label": "Total number of errors" } ],
            [ { "expression": "SUM(METRICS(\"warnings\"))", "id": "e2", "label": "Total number of warnings" } ]
          ],
          "period": 60,
          "region": "eu-west-2",
          "stacked": false,
          "stat": "Sum",
          "title": "# Errors and Warnings in external faced lambdas",
          "view": "timeSeries"
        }
      },
      {
        "type": "metric",
        "x": 0,
        "y": 6,
        "width": 24,
        "height": 6,
        "properties":
        {
          "metrics": [
            %{for lambda in local.app_interface_group~}
              [ "ErrorLogCount", "${lambda}-errors", { "id" : "${replace(lambda, "-", "")}_errors" } ],
              [ "WarningLogCount", "${lambda}-warnings", { "id" : "${replace(lambda, "-", "")}_warnings" } ],
            %{endfor~}
            [ { "expression": "SUM(METRICS(\"warnings\"))", "id": "e1", "label": "Total number of warnings" } ],
            [ { "expression": "SUM(METRICS(\"errors\"))", "id": "e2", "label": "Total number of errors" } ]
          ],
          "period": 60,
          "region": "eu-west-2",
          "stacked": false,
          "stat": "Sum",
          "title": "# Errors and Warnings in app facing lambdas",
          "view": "timeSeries"
        }
      },
      {
        "type": "log",
        "x": 0,
        "y": 12,
        "width": 8,
        "height": 6,
        "properties": {
          "query": "%{for source in local.app_interface_error_query} ${source} %{endfor~} fields @timestamp, @message\n| sort @timestamp desc\n| filter @message like /Status Code: 429/| stats count(*) by bin(1m)",
          "region": "eu-west-2",
          "stacked": false,
          "title": "App interface - Number of 429 ERRORS",
          "view": "timeSeries"
        }
      },
      {
        "type": "log",
        "x": 8,
        "y": 12,
        "width": 8,
        "height": 6,
        "properties": {
          "query": "%{for source in local.app_interface_error_query} ${source} %{endfor~} fields @timestamp, @message\n| sort @timestamp desc\n| filter @message like /Status Code: 5/| stats count(*) by bin(1m)",
          "region": "eu-west-2",
          "stacked": false,
          "title": "App interface - Number of 5XX ERRORS",
          "view": "timeSeries"
        }
      },
      {
        "type": "log",
        "x": 16,
        "y": 12,
        "width": 8,
        "height": 6,
        "properties": {
          "query": "%{for source in local.app_interface_error_query} ${source} %{endfor~} fields @timestamp, @message\n| sort @timestamp desc\n| filter @message like /Status Code: 403/| stats count(*) by bin(1m)",
          "region": "eu-west-2",
          "stacked": false,
          "title": "App interface - Number of 403 ERRORS",
          "view": "timeSeries"
        }
      },
      {
        "type": "log",
        "x": 0,
        "y": 18,
        "width": 8,
        "height": 6,
        "properties": {
          "query": "%{for source in local.external_interface_error_query} ${source} %{endfor~} fields @timestamp, @message\n| sort @timestamp desc\n| filter @message like /Status Code: 429/| stats count(*) by bin(1m)",
          "region": "eu-west-2",
          "stacked": false,
          "title": "External interface - Number of 429 ERRORS",
          "view": "timeSeries"
        }
      },
      {
        "type": "log",
        "x": 8,
        "y": 18,
        "width": 8,
        "height": 6,
        "properties": {
          "query": "%{for source in local.external_interface_error_query} ${source} %{endfor~} fields @timestamp, @message\n| sort @timestamp desc\n| filter @message like /Status Code: 5/| stats count(*) by bin(1m)",
          "region": "eu-west-2",
          "stacked": false,
          "title": "External interface - Number of 5XX ERRORS",
          "view": "timeSeries"
        }
      },
      {
        "type": "log",
        "x": 16,
        "y": 18,
        "width": 8,
        "height": 6,
        "properties": {
          "query": "%{for source in local.external_interface_error_query} ${source} %{endfor~} fields @timestamp, @message\n| sort @timestamp desc\n| filter @message like /Status Code: 403/| stats count(*) by bin(1m)",
          "region": "eu-west-2",
          "stacked": false,
          "title": "External interface - Number of 403 ERRORS",
          "view": "timeSeries"
        }
      },
      {
        "type": "log",
        "x": 0,
        "y": 24,
        "width": 8,
        "height": 6,
        "properties": {
          "query": "%{for source in local.internal_processing_error_query} ${source} %{endfor~} fields @timestamp, @message\n| sort @timestamp desc\n| filter @message like /Status Code: 429/| stats count(*) by bin(1m)",
          "region": "eu-west-2",
          "stacked": false,
          "title": "Internal processing - Number of 429 ERRORS",
          "view": "timeSeries"
        }
      },
      {
        "type": "log",
        "x": 8,
        "y": 24,
        "width": 8,
        "height": 6,
        "properties": {
          "query": "%{for source in local.internal_processing_error_query} ${source} %{endfor~} fields @timestamp, @message\n| sort @timestamp desc\n| filter @message like /Status Code: 5/| stats count(*) by bin(1m)",
          "region": "eu-west-2",
          "stacked": false,
          "title": "Internal processing - Number of 5XX ERRORS",
          "view": "timeSeries"
        }
      },
      {
        "type": "log",
        "x": 16,
        "y": 24,
        "width": 8,
        "height": 6,
        "properties": {
          "query": "%{for source in local.internal_processing_error_query} ${source} %{endfor~} fields @timestamp, @message\n| sort @timestamp desc\n| filter @message like /Status Code: 403/| stats count(*) by bin(1m)",
          "region": "eu-west-2",
          "stacked": false,
          "title": "Internal processing - Number of 403 ERRORS",
          "view": "timeSeries"
        }
      },
      {
        "type": "metric",
        "x": 0,
        "y": 30,
        "width": 24,
        "height": 6,
        "properties": {
          "metrics": [
            %{for gateway in var.gateways}
              [ "AWS/ApiGateway", "Count", "ApiId", "${gateway}", { "region": "eu-west-2", "visible": false, "id": "g_${gateway}_gateway"} ],
            %{endfor~}
              [ { "expression": "SUM(METRICS(\"gateway\"))", "label": "API ", "id": "g1"} ],
            %{for lambda in var.request_triggered}
              [ "AWS/Lambda", "Invocations", "FunctionName", "${lambda}", { "visible": false, "id": "${replace(lambda, "-", "")}_requestTriggered" } ],
            %{endfor~}
            [ { "expression": "SUM(METRICS(\"requestTriggered\"))", "label": "Request triggered lambdas", "id": "e1"} ],
            [ { "expression": "g1-e1", "label": "Difference", "id": "e2" } ]
          ],
          "view": "timeSeries",
          "stacked": false,
          "region": "eu-west-2",
          "stat": "Sum",
          "period": 60,
          "title": "Number of requests / Number of lambda invocations"
        }
      }
    ]
  }
EOF
}

resource "aws_cloudwatch_dashboard" "account_s3_bucket_dashboard" {
  dashboard_name = "${var.env}-S3-buckets-monitoring"
  dashboard_body = <<EOF
  {
   "start": "-P1W",
   "periodOverride": "auto",
   "widgets": [
      {
        "type": "metric",
        "x": 0,
        "y": 0,
        "width": 12,
        "height": 6,
        "properties":
        {
          "metrics": [
            %{for bucket in var.monitored_buckets~}
              [ "AWS/S3", "BucketSizeBytes", "StorageType", "StandardStorage", "BucketName", "${bucket}", { "id": "m1", "visible": false } ],
            %{endfor~}
            [ { "expression": "SUM(METRICS())", "label": "Storage size", "id": "e1", "stat": "Sum" } ]
          ],
          "view": "timeSeries",
          "stacked": false,
          "region": "eu-west-2",
          "period": 60,
          "stat": "Sum",
          "title": "Overall storage size in S3 buckets",
          "yAxis": {
              "left": {
                  "label": "Bytes",
                  "showUnits": false
              }
          }
        }
      },
      {
        "type": "metric",
        "x": 12,
        "y": 0,
        "width": 12,
        "height": 6,
        "properties":
        {
          "metrics": [
            %{for bucket in var.monitored_buckets~}
              [ "AWS/S3", "NumberOfObjects", "StorageType", "AllStorageTypes", "BucketName", "${bucket}", { "id": "m1", "visible": false } ],
            %{endfor~}
            [ { "expression": "SUM(METRICS())", "label": "Number of items", "id": "e1", "stat": "Sum" } ]
          ],
          "view": "timeSeries",
          "stacked": false,
          "region": "eu-west-2",
          "period": 60,
          "stat": "Sum",
          "title": "Number of items in S3 buckets",
          "yAxis": {
              "left": {
                  "label": "Count",
                  "showUnits": false
              }
          }
        }
      }
    ]
  }
  EOF
}

resource "aws_cloudwatch_dashboard" "cloudfront_dashboard" {
  dashboard_name = "${var.env}-CloudFront-monitoring"
  dashboard_body = <<EOF
  {
   "start": "-P1D",
   "periodOverride": "auto",
   "widgets": [
        ${templatefile("${path.module}/templates/cloudfront_upload_dashboard.tmpl", { api_distribution_id = var.cloudfront_upload_id, api_name = "Upload" })},
        ${templatefile("${path.module}/templates/cloudfront_distribution_dashboard.tmpl", { api_distribution_id = var.cloudfront_distribution_id, api_name = "Distribution" })},
        ${templatefile("${path.module}/templates/cloudfront_submission_dashboard.tmpl", { api_distribution_id = var.cloudfront_submission_id, api_name = "Submission" })}
   ]
 }
 EOF
}
