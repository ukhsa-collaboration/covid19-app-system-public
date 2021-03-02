variable "logs_bucket_id" {
  description = "The name of the bucket to which all S3 access logs are saved."
}

variable "force_destroy_s3_buckets" {
  description = "Force destroy s3 buckets if set to true"
  type        = bool
  default     = true
}

variable "policy_document" {
  description = "An aws_iam_policy_document to be attached to the s3 bucket"
  type = object({
    json = string
  })
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "lambda_project" {
  description = "The project from where this lambda belongs to"
}

variable "lambda_zip_path" {
  description = "The lambda path to the jar being stored in this repository"
}
