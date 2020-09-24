variable "name" {
  description = "The name of the processing module."
}

variable "lambda_handler_class" {
  description = "The name of the handler function used for this processing module"
}

variable "input_store" {
  description = "The name of the S3 bucket containing the data upon which to make the athena table"
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which S3 access logs are saved."
}

variable "lambda_repository_bucket" {
  description = "The name of the bucket which contains the lambda jar"
}

variable "lambda_object_key" {
  description = "The object key of the lambda jar in the s3 bucket"
}

variable "database_name" {
  description = "The name of the database"
}

variable "table_name" {
  description = "The name of the table"
}

variable "force_destroy_s3_buckets" {
  description = "Force destroy s3 buckets if set to true"
}

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}