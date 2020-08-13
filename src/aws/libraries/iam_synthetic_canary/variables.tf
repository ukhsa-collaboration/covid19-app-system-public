variable "service" {
  description = "The name of the service being provisioned"
  type        = string
  default     = "lambda"
}

variable "name" {
  type    = string
  default = "exec"
}
