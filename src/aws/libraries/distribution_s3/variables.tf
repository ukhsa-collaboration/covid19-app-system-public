variable "name" {
  description = "The name of the distribution module. This should correspond to the API contract"
  type        = string
}

variable "service" {
  description = "The name of the service to provision. This should be distribution or submission"
  type        = string
}

variable "origin_access_identity_path" {
  description = "ARN from the Origin Access Identity"
  type        = string
}

#variable "origin_access_identity" {
#  description = "Origin Access Identity"
#}

variable "logs_bucket_id" {
  description = "The name of the bucket to which S3 access logs are saved."
}
