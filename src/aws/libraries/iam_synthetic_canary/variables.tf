variable "region" {
  description = "The AWS region name in which the canaries are deployed"
  type        = string
}

variable "service" {
  description = "The name of the service being provisioned"
  type        = string
  default     = "lambda"
}

variable "name" {
  type    = string
  default = "exec"
}
