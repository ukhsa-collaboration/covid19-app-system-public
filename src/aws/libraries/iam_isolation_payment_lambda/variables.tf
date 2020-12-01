variable "name" {
  description = "The name for the IAM role"
}

variable "consume_function_arn" {
  description = "Lambda ARN"
}

variable "verify_function_arn" {
  description = "Lambda ARN"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "isolation_payment_gateway_role_trust_policy_principal" {
  description = "Trust policy principal (IAM Role in Isolation Payment Gateway Account)"
  type        = list(string)
}
