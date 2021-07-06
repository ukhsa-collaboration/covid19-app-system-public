variable "lambda_function_name" {
  description = "the name of the function"
}

variable "lambda_handler_class" {
  description = "The function or class name of the handler"
}

variable "lambda_runtime" {
  description = "The runtime version and language"
}

variable "lambda_timeout" {
  description = "The maximum time the function will run, in seconds"
}

variable "lambda_memory" {
  description = "The memory allocation to the function"
}

variable "lambda_execution_role_arn" {
  description = "The role used by the function"
}

variable "tags" {
}

variable "lambda_environment_variables" {

}

variable "lambda_log_group_retention_in_days" {
}

variable "app_alarms_topic" {
  description = "the topic which receives metric alarms"
}

variable "invocations_alarm_enabled" {
  description = "whether or not to enable the invocation alarm"
}
