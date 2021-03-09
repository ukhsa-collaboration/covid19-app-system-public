output "analytics_bucket_arn" {
  value = module.analytics_store.bucket_arn
}

output "lambda_function_name" {
  value = module.analytics_lambda.lambda_function_name
}
