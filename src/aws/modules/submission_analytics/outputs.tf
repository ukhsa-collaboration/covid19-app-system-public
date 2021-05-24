output "endpoint" {
  value = module.submission_gateway.api_endpoint
}

output "gateway_id" {
  value = module.submission_gateway.api_gateway_id
}

output "submission_lambda_function_name" {
  value = aws_lambda_function.ingest.function_name
}

output "processing_lambda_function_name" {
  value = module.processing_lambda.lambda_function_name
}