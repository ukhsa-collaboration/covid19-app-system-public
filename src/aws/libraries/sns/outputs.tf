output "topic" {
  value = aws_sns_topic.this
}

output "sns_topic_arn" {
  value = aws_sns_topic.this.arn
}
