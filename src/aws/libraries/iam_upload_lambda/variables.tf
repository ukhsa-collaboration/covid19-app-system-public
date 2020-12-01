variable "name" {
  description = "The name for the IAM role"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}