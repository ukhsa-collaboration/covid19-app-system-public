output "endpoint" {
  value = module.isolation_payment_api.api_endpoint
}

output "gateway_id" {
  value = module.isolation_payment_api.api_gateway_id
}

output "isolation_payment_lambda_function_name" {
  value = module.isolation_payment_api_lambda.lambda_function_name
}
