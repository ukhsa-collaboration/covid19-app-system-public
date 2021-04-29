resource "aws_sqs_queue" "this" {
  name                       = "mock-venue-history-queue"
  visibility_timeout_seconds = 180
  message_retention_seconds  = 691200 // 8 days
  receive_wait_time_seconds  = 0
}

output "mock_venue_history_queue_url" {
  value = aws_sqs_queue.this.id
}
