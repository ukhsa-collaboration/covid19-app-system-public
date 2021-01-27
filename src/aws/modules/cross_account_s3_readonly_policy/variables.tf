variable "prefix" {
  description = "The prefix to differentiate sid on policy statements"
  type        = string
}

variable "principal_aws_accounts" {
  description = "List of principal aws account numbers to enable readonly access to the bucket"
  type        = list(string)
}

variable "s3_bucket_arn" {
  description = "S3 bucket arn to add access to"
  type        = string
}
