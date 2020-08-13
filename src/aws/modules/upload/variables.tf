variable "name" {
  description = "The name of the API"
}

variable "bucket_name" {
  description = "The distribution API store bucket where the upload API persists its data"
}

variable "distribution_id" {
  description = "The CloudFront distribution ID used for cache invalidation"
}

variable "distribution_invalidation_pattern" {
  description = "The path pattern to use for cache invalidation"
}

variable "lambda_handler_class" {
  description = "The full class name for the lambda entry point (handler)"
}

variable "rate_limit" {
  description = "The requests per second limit for this API gateway"
}

variable "burst_limit" {
  description = "The requests per second burst limit for this API gateway"
}

variable "lambda_repository_bucket" {
  description = "The name of the bucket which contains the lambda jar"
}

variable "lambda_object_key" {
  description = "The object key of the lambda jar in the s3 bucket"
}
