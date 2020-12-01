output "upload_lambda_function" {
  value = module.processing_upload_lambda.lambda_function_name
}
output "download_lambda_function" {
  value = module.processing_download_lambda.lambda_function_name
}