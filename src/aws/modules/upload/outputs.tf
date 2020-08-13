output "api_endpoint" {
  value = module.upload_gateway.api_endpoint
}

output "lambda_function_name" {
  value = module.upload_lambda.lambda_function_name
}

output "gateway_id" {
  value = module.upload_gateway.api_gateway_id
}