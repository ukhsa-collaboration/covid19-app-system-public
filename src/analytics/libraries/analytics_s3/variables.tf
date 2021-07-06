variable "name" {
  description = "The name you want to use to identify this component in the system."
}

variable "versioning" {
  default     = false
  description = "true or false"
}

variable "service" {
  description = "The name of a component providing a certain service. This could be another App-System service or an AWS service."
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which S3 access logs are saved."
}

variable "force_destroy_s3_buckets" {
  description = "Force destroy s3 bucket if set to true"
}

variable "enable_versioning" {
  description = "Enable versioning on the bucket"
}

variable "lifecycle_rules" {
  description = "Lifecycle rules"
  type = list(object({
    id      = string,
    prefix  = string,
    enabled = bool,
    days    = number
  }))
  default = []
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}
