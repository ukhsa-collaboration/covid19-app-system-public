variable "env" {
  description = "environment reference"
}

variable "advanced_analytics_function" {
  description = "name of lambda function"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}
