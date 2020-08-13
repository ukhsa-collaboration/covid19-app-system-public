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
