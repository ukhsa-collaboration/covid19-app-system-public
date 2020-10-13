output "function" {
  value = module.virology_tokens_processing_lambda.lambda_function_name
}

output "output_store" {
  value = module.virology_tokens_bucket.bucket_name
}