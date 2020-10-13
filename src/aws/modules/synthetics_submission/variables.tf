variable "region" {
  description = "Region in which the canaries will be deployed - ideally not the same as the service being monitored"
  type        = string
  default     = "eu-west-1"
}

variable "service" {
  description = "The name of the service to provision"
  type        = string
}

variable "lambda_exec_role_arn" {
  description = "The ARN of the role for executing the Lambda"
  type        = string
}

variable "base_domain" {
  description = "Name of the endpoint base domain in DNS (from main.tf in the appropriate subfolder of src/aws/accounts)"
  type        = string
}

variable "synthetic_schedule" {
  description = "How frequently to run the synthetic canary"
  type = object({
    duration_in_seconds = number
    expression          = string
  })
  default = {
    duration_in_seconds = 0 # forever
    expression          = "rate(5 minutes)"
  }
}

variable "api_gw_support" {
  type    = bool
  default = false
}

variable "lambda_timeout" {
  default = 3
}

variable "xray_enabled" {
  type        = bool
  description = "If true, set lambda tracing configuration to Active, else PassThrough"
  default     = true
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which S3 access logs are saved."
}

variable "dependency_ref" {
  description = "Supply reference from another resource/module if there's a need to build dependency. Replace with depends_on once migrate to Terraform v0.13 or later"
  type        = string
  default     = "_"
}
