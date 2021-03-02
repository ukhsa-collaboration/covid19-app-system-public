variable "logs_bucket_id" {
  description = "The name of the bucket to which all S3 access logs are saved"
}
variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "lambda_repository_bucket" {
  description = "The name of the bucket which contains the lambda jar"
}

variable "lambda_object_key" {
  description = "The object key of the lambda jar in the s3 bucket"
}

variable "circuit_breaker_log_group_name" {
  description = "The log group from where we get the logs fro"
}
variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}

variable "policy_document" {
  description = "An aws_iam_policy_document to be attached to the s3 bucket"
  type = object({
    json = string
  })
}

