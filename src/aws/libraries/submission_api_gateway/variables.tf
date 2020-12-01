variable "name" {
  description = "The name of the API"
}

variable "lambda_function_name" {
  description = "The lambda function linked to the gateway"
}

variable "lambda_function_version" {
  description = "The version of the lambda function linked to the gateway (or 0 if versioning is not enabled for the lambda)"
  type        = number
  default     = 0
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

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}