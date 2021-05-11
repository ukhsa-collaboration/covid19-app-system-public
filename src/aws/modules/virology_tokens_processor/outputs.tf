output "tokens_processing_function" {
  value = module.virology_tokens_processing_lambda.lambda_function_name
}

output "scheduled_tokens_processing_function" {
  value = module.scheduled_virology_tokens_generating_lambda.lambda_function_name
}

output "output_store" {
  value = module.virology_tokens_bucket.bucket_name
}

output "output_store_arn" {
  value = module.virology_tokens_bucket.bucket_arn
}

output "sms_topic_arn" {
  value = module.virology_tokens_sms_topic.sns_topic_arn
}

output "email_topic_arn" {
  value = module.virology_tokens_mail_topic.sns_topic_arn
}
