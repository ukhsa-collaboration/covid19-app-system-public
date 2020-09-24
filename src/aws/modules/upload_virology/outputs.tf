output "api_endpoint" {
  value = module.upload_gateway.api_endpoint
}

output "submission_tokens_table" {
  value = aws_dynamodb_table.submission_tokens.id
}

output "results_table" {
  value = aws_dynamodb_table.test_results.id
}

output "test_orders_table" {
  value = aws_dynamodb_table.test_orders.id
}

output "lambda_function_name" {
  value = module.upload_lambda.lambda_function_name
}

output "gateway_id" {
  value = module.upload_gateway.api_gateway_id
}

output "test_orders_index_name" {
  value = "${local.identifier_prefix}-ordertokens-index"
}