output "order_lambda_function_name" {
  value = module.isolation_payment_order_lambda.lambda_function_name
}

output "verify_lambda_function_name" {
  value = module.isolation_payment_verify_lambda.lambda_function_name
}

output "consume_lambda_function_name" {
  value = module.isolation_payment_consume_lambda.lambda_function_name
}

output "endpoint" {
  value = module.isolation_payment_gateway.api_endpoint
}

output "gateway_id" {
  value = module.isolation_payment_gateway.api_gateway_id
}

output "ipc_tokens_table" {
  value = aws_dynamodb_table.isolation_payment_tokens_table.id
}

output "gateway_role" {
  value = module.isolation_payment_gateway_role.arn
}