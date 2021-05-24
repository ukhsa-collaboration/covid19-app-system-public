output "api_endpoint" {
  value = module.test_order_gateway.api_endpoint
}

output "lambda_function_name" {
  value = module.test_order_lambda.lambda_function_name
}

output "gateway_id" {
  value = module.test_order_gateway.api_gateway_id
}

output "log_group" {
  value = module.test_order_lambda.lambda_log_group
}
