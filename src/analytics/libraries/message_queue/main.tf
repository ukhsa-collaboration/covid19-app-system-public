resource "aws_sqs_queue" "this" {
  name                       = var.name
  visibility_timeout_seconds = 180
  message_retention_seconds  = 691200 // 8 days
  receive_wait_time_seconds  = 0
  delay_seconds              = var.message_delivery_delay
  tags                       = var.tags
  redrive_policy = var.enable_dead_letter_queue ? jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq[0].arn
    maxReceiveCount     = 5
  }) : ""
}

resource "aws_sqs_queue" "dlq" {
  count                      = var.enable_dead_letter_queue ? 1 : 0
  name                       = "${var.name}-dlq"
  visibility_timeout_seconds = 180
  message_retention_seconds  = 1209600
  receive_wait_time_seconds  = 20
  tags                       = var.tags
}

resource "aws_cloudwatch_metric_alarm" "dlq" {
  count               = var.enable_dead_letter_queue ? 1 : 0
  alarm_name          = "${var.name}-dlq"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = "120"
  statistic           = "Sum"
  threshold           = "1"
  alarm_description   = "This metric monitors whether any events are sent to the ${var.name} dead letter queue. Refer to runbook (<https://github.com/nihp-public/COVID19-app-system/blob/master/doc/ops/run_books/RBK015-dead-letter-queue-alarm.adoc|RBK015>) for resolution details."
  alarm_actions       = [var.dead_letter_queue_alarm_topic_arn]
  treat_missing_data  = "notBreaching"
  tags                = var.tags
  dimensions          = { QueueName = aws_sqs_queue.dlq[0].name }
}
