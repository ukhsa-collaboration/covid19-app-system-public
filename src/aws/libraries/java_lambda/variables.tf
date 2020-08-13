variable "lambda_function_name" {
  description = "The name of the lambda function"
}

variable "lambda_repository_bucket" {
  description = "The s3 bucket containing the lambda code"
}

variable "lambda_object_key" {
  description = "The object key for the lambda code within the repository s3 bucket"
}

variable "lambda_handler_class" {
  description = "The full class name for the lambda entry point (handler)"
}

variable "lambda_execution_role_arn" {
  description = "The IAM role to assign to the lambda"
}

variable "lambda_timeout" {
  description = "The timeout for the lambda execution"
}
variable "lambda_memory" {
  description = "The ammount of memory to allocate for the lambda"
}

variable "lambda_environment_variables" {
  description = "A map of environment variable --> value to pass as environment to the lambda"
  type        = map
}