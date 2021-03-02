variable "policy_type" {
  description = "Type of policy to return (required one of secure_origin_access, cross_account_readonly or default)"
  type        = string
}

variable "s3_bucket_arn" {
  description = "S3 bucket arn to add access to (required)"
  type        = string
}

variable "prefix" {
  description = "The prefix to differentiate sid on policy statements (required for cross_account_readonly)"
  type        = string
  default     = ""
}

variable "principal_aws_accounts" {
  description = "List of principal aws account numbers to enable readonly access to the bucket (required for cross_account_readonly)"
  type        = list(string)
  default     = []
}

variable "origin_access_identity_arn" {
  description = "ARN from the Origin Access Identity (required for secure_origin_access or cross_account_readonly)"
  type        = string
  default     = ""
}
