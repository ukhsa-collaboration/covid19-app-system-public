variable "enabled" {
  description = "Set to false if this module is to be suppressed"
  type        = bool
  default     = true
}

variable "region" {
  description = "Region in which the canary will be deployed - ideally not the same as the service being monitored"
  type        = string
}

variable "service" {
  description = "The name of the service to provision"
  type        = string
}

variable "name" {
  description = "Name of the canary function. No more than 9 characters long, only lowercase letters, digits, _ and -"
  type        = string
  default     = "df-canary"
}

variable "synthetic_script_path" {
  description = "Node.js code of the lambda to be run as the canary"
  type        = string
}

variable "base_domain" {
  description = "Name of the endpoint base domain in DNS (from main.tf in the appropriate subfolder of src/aws/accounts)"
  type        = string
}

variable "hostname" {
  description = "Name of the endpoint host in DNS (e.g. upload-te-ci or submission-08abcd)"
  type        = string
}

variable "uri_path" {
  description = "pathname part of the URL of the endpoint to be probed (see also hostname and base_domain)"
  type        = string
}

variable "method" {
  description = "HTTP method to be used"
  type        = string
  default     = "GET"
}

variable "secret_name" {
  description = "AWS secret manager name needed to retrieve authorization header for endpoint"
  type        = string
}

variable "expc_status" {
  type        = string
  description = "status code expected to be returned by the endpoint if invalid authentication headers are sent"
}

variable "synthetic_vpc_config" {
  description = "Security Group IDs and Subnet IDs of VPC in which the canary is to run"
  type = object({
    security_group_ids = list(string)
    subnet_ids         = list(string)
  })
  default = {
    security_group_ids = []
    subnet_ids         = []
  }
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

variable "lambda_exec_role_arn" {
  description = "The ARN of the iam role for Lambda function"
  type        = string
}

variable "artifact_s3_bucket" {
  description = "Name of the S3 bucket where the artifact zip file will be stored for the Lambda"
  type        = string
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
  default     = false
}

variable "dependency_ref" {
  description = "Supply reference from another resource/module if there's a need to build dependency. Replace with depends_on once migrate to Terraform v0.13 or later"
  type        = string
  default     = "_"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}
