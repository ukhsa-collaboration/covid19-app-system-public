variable "name" {
  description = "The name of the submission module. This should correspond to the API contract"
}

variable "service" {
  description = "The name of the service to provision. This should be distribution, processing, submission or upload"
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which S3 access logs are saved."
}

variable "origin_access_identity_path" {
  description = "The Cloud Front access identity to be associated with this bucket"
}
