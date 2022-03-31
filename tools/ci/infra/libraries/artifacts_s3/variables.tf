variable "name" {
  description = "The name of the S3 bucket"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
  default     = {}
}

variable "lifecycle_rules" {
  description = "Lifecycle rules"
  type = list(object({
    id      = string,
    prefix  = string,
    enabled = bool,
    days    = number
  }))
  default = []
}
