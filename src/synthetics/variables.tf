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
