output "function" {
  value = module.submission_lambda.lambda_function_name
}

output "version" {
  value = module.submission_lambda.version
}

output "endpoint" {
  value = module.submission_gateway.api_endpoint
}

output "gateway_id" {
  value = module.submission_gateway.api_gateway_id
}

output "log_group" {
  value = module.submission_lambda.lambda_log_group
}
