locals {
  trigger_export_processor_identifier = "${terraform.workspace}-edge-trigger-export-proc"
  data_upload_processor_identifier    = "${terraform.workspace}-edge-data-upload-proc"
  enable_edge_export                  = contains(var.enabled_workspaces, terraform.workspace)
}

module "trigger_export_processor_role" {
  source = "../../../aws/libraries/iam_processing_lambda"
  name   = local.trigger_export_processor_identifier
  tags   = var.tags
}

module "trigger_export_processing_lambda" {
  source                    = "../../../aws/libraries/java_lambda"
  lambda_function_name      = local.trigger_export_processor_identifier
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.analyticsedge.export.TriggerExportHandler"
  lambda_execution_role_arn = module.trigger_export_processor_role.arn
  lambda_timeout            = 60
  lambda_memory             = 512
  lambda_environment_variables = {
    export_bucket_name     = module.edge_export_store.bucket_name
    mobile_analytics_table = var.mobile_analytics_table
  }
  log_retention_in_days     = var.log_retention_in_days
  app_alarms_topic          = var.alarm_topic_arn
  tags                      = var.tags
  invocations_alarm_enabled = false
}

resource "aws_cloudwatch_event_rule" "every_day" {
  count               = local.enable_edge_export ? 1 : 0
  name                = "${local.trigger_export_processor_identifier}-every-day"
  schedule_expression = "cron(0 7 ? * * *)"
}

resource "aws_cloudwatch_event_target" "target_lambda" {
  count = local.enable_edge_export ? 1 : 0
  rule  = aws_cloudwatch_event_rule.every_day[0].name
  arn   = module.trigger_export_processing_lambda.lambda_function_arn
}

resource "aws_lambda_permission" "cloudwatch_invoke_lambda_permission" {
  count         = local.enable_edge_export ? 1 : 0
  action        = "lambda:InvokeFunction"
  function_name = module.trigger_export_processing_lambda.lambda_function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.every_day[0].arn
}

resource "aws_lambda_function_event_invoke_config" "lambda_function_config" {
  function_name          = module.trigger_export_processing_lambda.lambda_function_name
  maximum_retry_attempts = 0
}

module "edge_export_store" {
  source                   = "../../libraries/analytics_s3"
  name                     = "edge-export" //TODO - revisit the name
  service                  = "edge"        // would like to verify this
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_versioning        = false
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

module "data_upload_queue" {
  source                            = "../../libraries/message_queue"
  name                              = "${terraform.workspace}-edge-upload-message-queue"
  message_delivery_delay            = 0
  enable_dead_letter_queue          = true
  dead_letter_queue_alarm_topic_arn = var.alarm_topic_arn
  tags                              = var.tags
}

resource "aws_sqs_queue_policy" "data_upload_queue_policy" {
  queue_url = module.data_upload_queue.queue_url
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
      "Resource": "${module.data_upload_queue.queue_arn}",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "${aws_cloudwatch_event_rule.data_upload_event_rule.arn}"
        }
      }
    }
  ]
}
POLICY
}

resource "aws_cloudwatch_event_rule" "data_upload_event_rule" {
  name = local.data_upload_processor_identifier
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
      "PutObject",
      "CompleteMultipartUpload"
    ],
    "requestParameters": {
      "bucketName": [
        "${module.edge_export_store.bucket_name}"
      ]
    }
  }
}
EOF
}

resource "aws_cloudwatch_event_target" "data_upload_event_target" {
  arn  = module.data_upload_queue.queue_arn
  rule = aws_cloudwatch_event_rule.data_upload_event_rule.id
  input_transformer {
    input_paths = {
      bucketName = "$.detail.requestParameters.bucketName",
      key        = "$.detail.requestParameters.key"
    }
    input_template = <<EOF
{
  "target": "${module.data_upload_processing_lambda.lambda_function_name}",
  "version": 1,
  "bucketName": <bucketName>,
  "key": <key>
}
EOF
  }
}

module "data_upload_processor_role" {
  source = "../../../aws/libraries/iam_processing_lambda"
  name   = local.data_upload_processor_identifier
  tags   = var.tags
}

module "data_upload_processing_lambda" {
  source                    = "../../../aws/libraries/java_lambda"
  lambda_function_name      = local.data_upload_processor_identifier
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.analyticsedge.upload.EdgeDataUploadHandler"
  lambda_execution_role_arn = module.data_upload_processor_role.arn
  lambda_timeout            = 180
  lambda_memory             = 1024
  lambda_environment_variables = {
    TARGET_URL            = var.edge_export_url
    SAS_TOKEN_SECRET_NAME = "/edge/azure_storage_container/sas-token"
  }
  log_retention_in_days     = var.log_retention_in_days
  app_alarms_topic          = var.alarm_topic_arn
  tags                      = var.tags
  invocations_alarm_enabled = false
}

resource "aws_lambda_event_source_mapping" "event_source_mapping_data_upload" {
  batch_size       = 1
  event_source_arn = module.data_upload_queue.queue_arn
  function_name    = module.data_upload_processing_lambda.lambda_function_arn
}

