variable "name" {
  description = "Function name suffix"
}

variable "lambda_repository_bucket" {
  description = "The name of the bucket which contains the lambda jar"
}

variable "lambda_object_key" {
  description = "The object key of the lambda jar in the s3 bucket"
}

variable "log_retention_in_days" {
  description = "Days for which events in the associated CloudWatch log group are retained. 0 (the default) means forever"
  type        = number
  default     = 0
}

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "analytics_submission_store" {
  description = "Name of the bucket containing files to be uploaded to AAE - triggered by PutObject"
}

variable "enabled_workspaces" {
  description = "Target environments with enabled SQS processing (allowed values: te-<env>, *, branch)"
  type        = list(string)
}

variable "aae_url_prefix" {
  description = "HTTPS PUT target"
}

variable "aae_url_suffix" {
  description = "HTTPS PUT target (e.g. empty string)"
}

variable "p12_cert_secret_name" {
  description = "Name of the SecretsManager secret containing the TLS client cert in (binary secret: .p12 format)"
}

variable "p12_cert_password_secret_name" {
  description = "Name of the SecretsManager secret containing the password of the TLS client cert (string secret)"
}

variable "aae_subscription_secret_name" {
  description = "Name of the SecretsManager secret containing the Ocp-Apim-Subscription-Key HTTP header value (string secret)"
}
