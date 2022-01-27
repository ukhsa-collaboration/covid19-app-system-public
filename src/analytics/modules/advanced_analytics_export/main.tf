locals {
  identifier_prefix = "${terraform.workspace}-${var.name}"
}

module "processor_role" {
  source = "../../../aws/libraries/iam_processing_lambda"
  name   = local.identifier_prefix
  tags   = var.tags
}

module "processing_lambda" {
  source                         = "../../../aws/libraries/java_lambda"
  lambda_function_name           = local.identifier_prefix
  lambda_repository_bucket       = var.lambda_repository_bucket
  lambda_object_key              = var.lambda_object_key
  lambda_handler_class           = "uk.nhs.nhsx.aae.AAEUploadHandler"
  lambda_execution_role_arn      = module.processor_role.arn
  lambda_timeout                 = 30
  lambda_memory                  = 512
  reserved_concurrent_executions = 100
  lambda_environment_variables = {
    AAE_URL_PREFIX                = var.aae_url_prefix
    AAE_URL_SUFFIX                = var.aae_url_suffix
    P12_CERT_SECRET_NAME          = var.p12_cert_secret_name
    P12_CERT_PASSWORD_SECRET_NAME = var.p12_cert_password_secret_name
    AAE_SUBSCRIPTION_SECRET_NAME  = var.aae_subscription_secret_name
    ANALYTICS_EVENTS_BUCKET       = var.analytics_events_bucket
    ANALYTICS_DATA_BUCKET         = var.analytics_data_bucket

    # [PARQUET CONSOLIDATION] FIXME (before delta-migration)
    S3_DISALLOWED_PREFIX_LIST = "submitteddatehour=__HIVE_DEFAULT_PARTITION__,_temporary,submitteddatehour=/2021/03/FIXME/FIXME"
  }
  log_retention_in_days     = var.log_retention_in_days
  app_alarms_topic          = var.alarm_topic_arn
  tags                      = var.tags
  invocations_alarm_enabled = false
}

resource "aws_sqs_queue" "this" {
  name                       = local.identifier_prefix
  visibility_timeout_seconds = 180
  message_retention_seconds  = 1209600
  receive_wait_time_seconds  = 20
  tags                       = var.tags
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = 5
  })
}

resource "aws_sqs_queue" "dlq" {
  name                       = "${local.identifier_prefix}-dlq"
  visibility_timeout_seconds = 180
  message_retention_seconds  = 1209600
  receive_wait_time_seconds  = 20
  tags                       = var.tags
}

resource "aws_cloudwatch_metric_alarm" "age_of_oldest_message_alarm" {
  alarm_name          = "${local.identifier_prefix}-age_of_oldest_message"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "ApproximateAgeOfOldestMessage"
  namespace           = "AWS/SQS"
  period              = "60"
  statistic           = "Maximum"
  threshold           = "3600" # one hour
  alarm_description   = "This metric monitors age of oldest message in the queue. In case it's one hour or more, it goes in ALARM state."
  alarm_actions       = [var.alarm_topic_arn]
  treat_missing_data  = "notBreaching"
  tags                = var.tags
  dimensions          = { QueueName = aws_sqs_queue.this.name }
}

resource "aws_cloudwatch_metric_alarm" "dlq" {
  alarm_name          = "${local.identifier_prefix}-dlq"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = "120"
  statistic           = "Sum"
  threshold           = "1"
  alarm_description   = "This metric monitors whether any events are sent to the ${local.identifier_prefix} dead letter queue. Refer to runbook (<https://github.com/nihp-public/covid19-app-system-public/blob/master/doc/ops/run_books/RBK015-dead-letter-queue-alarm.adoc|RBK015>) for resolution details."
  alarm_actions       = [var.alarm_topic_arn]
  treat_missing_data  = "notBreaching"
  tags                = var.tags
  dimensions          = { QueueName = aws_sqs_queue.dlq.name }
}

resource "aws_lambda_event_source_mapping" "event_source_mapping" {
  batch_size       = 1
  event_source_arn = aws_sqs_queue.this.arn
  function_name    = module.processing_lambda.lambda_function_arn
}

resource "aws_sqs_queue_policy" "this" {
  queue_url = aws_sqs_queue.this.id
  policy    = <<POLICY
{
  "Version": "2008-10-17",
  "Id": "allow_eventbridge_sent",
  "Statement": [
    {
      "Sid": "1",
      "Effect": "Allow",
      "Principal": {
        "Service": "events.amazonaws.com"
      },
      "Action": "sqs:SendMessage",
      "Resource": "${aws_sqs_queue.this.arn}",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "${aws_cloudwatch_event_rule.this.arn}"
        }
      }
    }
  ]
}
POLICY
}

resource "aws_cloudwatch_event_rule" "this" {
  name = local.identifier_prefix
  tags = var.tags

  event_pattern = <<EOF
{
  "source": [
    "aws.s3"
  ],
  "detail-type": [
    "AWS API Call via CloudTrail"
  ],
  "detail": {
    "eventSource": [
      "s3.amazonaws.com"
    ],
    "eventName": [
      "PutObject"
    ],
    "requestParameters": {
      "bucketName": [
        "${var.analytics_submission_store}"
      ]
    }
  }
}
EOF
}

resource "aws_cloudwatch_event_target" "this" {
  arn  = aws_sqs_queue.this.arn
  rule = aws_cloudwatch_event_rule.this.id
  input_transformer {
    input_paths = {
      bucketName = "$.detail.requestParameters.bucketName",
      key        = "$.detail.requestParameters.key"
    }
    input_template = <<EOF
{
  "target": "${local.identifier_prefix}",
  "version": 1,
  "bucketName": <bucketName>,
  "key": <key>
}
EOF
  }
}
