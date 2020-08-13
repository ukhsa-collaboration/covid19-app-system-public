variable "name" {
  description = "The name of the S3 bucket. This should correspond to the API contract"
}

variable "default_payload" {
  description = "The S3 object to serve by default"
}

variable "payload_source" {
  description = "The content to distribute"
}

variable "metadata_signature" {
  description = "The metadata signature header value"
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which all S3 access logs are saved."
}

variable "metadata_signature_date" {
  description = "The metadata signature date header value"
}
