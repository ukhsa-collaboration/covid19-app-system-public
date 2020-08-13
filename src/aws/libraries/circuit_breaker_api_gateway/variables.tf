variable "name" {
  description = "The name of the API"
}

variable "lambda_function_name" {
  description = "The lambda function linked to the gateway"
}

variable "lambda_function_arn" {
  description = "The ARN through which the lambda function is invoked"
}

variable "rate_limit" {
  description = "The requests per second limit for this API gateway"
}

variable "burst_limit" {
  description = "The requests per second burst limit for this API gateway"
}