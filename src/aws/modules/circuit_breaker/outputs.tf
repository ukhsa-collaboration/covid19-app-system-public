output "function" {
  value = module.circuit_breaker_lambda.lambda_function_name
}

output "endpoint" {
  value = module.circuit_breaker_gateway.api_endpoint
}

output "gateway_id" {
  value = module.circuit_breaker_gateway.api_gateway_id
}

output "lambda_log_group" {
  value = module.circuit_breaker_lambda.lambda_log_group
}
