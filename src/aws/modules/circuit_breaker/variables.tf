variable "name" {
  description = "The name of the circuit breaker module"
}

variable "lambda_handler_class" {
  description = "The full classname for the handler function"
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

variable "ssm_parameter" {
  description = "Base name of the SSM Parameter used to control this circuit breaker"
}
