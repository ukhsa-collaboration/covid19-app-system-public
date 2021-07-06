variable "name" {
  description = "The name for the IAM role"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "statement_actions" {
  description = "List of policy actions you want the lambda function to be able to do"
  type        = list(any)
}

variable "resources" {
  description = "List of resources for the statement_actions"
  type        = list(any)
}
