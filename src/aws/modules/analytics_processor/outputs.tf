output "output_store" {
  value = module.output_store.bucket_name
}

output "output_store_id" {
  value = module.output_store.bucket_id
}

output "function" {
  value = module.processing_lambda.lambda_function_name
}
