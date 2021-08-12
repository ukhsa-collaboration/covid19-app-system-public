variable "tags" {}

variable "lambda_function_name" {
  description = "name given to the func"
}

variable "log_retention_in_days" {
  description = "how long to keep the logs of the func for"
}

variable "statement_actions" {
  description = "List of policy actions you want the lambda function to be able to do"
  type        = list(any)
}

variable "resources" {
  description = "List of resources for the statement_actions"
  type        = list(any)
}

variable "app_alarms_topic" {
  description = "the topic for which alarms will be sent"
}

variable "lambda_handler_class" {
  description = "the name of the file and handler function like handler.handler_function"
}

variable "permission_set_name" {
  description = "the name of the lambda role to be created as part of this feature"
}

variable "environment_variables" {
  description = "lambda func env variables"
}

variable "lambda_timeout" {
  description = "timeout for the lambda function"
}
