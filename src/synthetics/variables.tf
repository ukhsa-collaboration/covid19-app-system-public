variable "aws_account_name" {
  description = "The name of the account being deployed to (e.g. dev, staging, prod)"
  type        = string
}

variable "base_domain" {
  description = "The base DNS domain for the APIs"
  type        = string
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which all S3 access logs are saved"
}

variable "app_alarms_topic_arn" {
  description = "SNS topic to publish metric alerts to"
  type        = string
}

variable "canary_deploy_region" {
  description = "region in which canaries are to be deployed (needed to create an aws provider)"
  type        = string
}
