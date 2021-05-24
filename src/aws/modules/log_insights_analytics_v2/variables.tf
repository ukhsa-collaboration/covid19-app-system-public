variable "service" {
  description = "The name of the analytics service that provides this data"
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which all S3 access logs are saved"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "lambda_handler_class" {
  description = "The full class name for the lambda entry point (handler)"
}

variable "lambda_repository_bucket" {
  description = "The name of the bucket which contains the lambda jar"
}

variable "lambda_object_key" {
  description = "The object key of the lambda jar in the s3 bucket"
}

variable "force_destroy_s3_buckets" {
  description = "Force destroy s3 bucket if set to true"
}

variable "log_group_name" {
  description = "The log group of lambda to extract analytics from"
}

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}

variable "analytics_bucket_name" {
  description = "name of the bucket where output of log insights are to exported to be injested by athena"
}

variable "analytics_bucket_prefix" {
  description = "prefix to be appended to object key name"
}

variable "policy_document" {
  description = "An aws_iam_policy_document to be attached to the s3 bucket"
  type = object({
    json = string
  })
}
