output "lambda_function_name" {
  value = module.processing_lambda.lambda_function_id
}

output "lambda_function_arn" {
  value = module.processing_lambda.lambda_function_arn
}

output "event_source_arn" {
  value = aws_sqs_queue.this.arn
}