variable "base_domain" {
  description = "The base DNS domain for the APIs"
  type        = string
}

variable "rate_limit" {
  description = "The requests per second limit for this API gateway"
  type        = number
}

variable "burst_limit" {
  description = "The requests per second burst limit for this API gateway"
  type        = number
}
