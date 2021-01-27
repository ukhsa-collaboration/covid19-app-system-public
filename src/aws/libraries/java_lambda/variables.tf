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

variable "log_retention_in_days" {
  description = "Days for which events in the associated CloudWatch log group are retained. 0 (the default) means forever"
  type        = number
  default     = 0
}

variable "app_alarms_topic" {
  description = "Alarm topic arn"
  type        = string
}

variable "publish" {
  description = "Create lambda version (required for provisioned concurrency)"
  type        = bool
  default     = false
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "reserved_concurrent_executions" {
  default = -1
}

variable "invocations_alarm_enabled" {
  description = "Flag to enable the alarm for lambda invocations"
  type        = bool
  default     = true
}