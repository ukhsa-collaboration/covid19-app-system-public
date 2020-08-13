variable "name" {
  description = "The name for the IAM role"
}

variable "bucket_name" {
  description = "The name of the bucket associated with this lambda"
}

variable "certificate_secret_arn" {
  description = "The arn suffix of the certificate stored in secrets manager"
}

variable "key_secret_name_arn" {
  description = "The arn suffix of the private key stored in secrets manager"
}

variable "encryption_password_secret_name_arn" {
  description = "The arn suffix of the encryption password stored in secrets manager"
}

variable "subscription_key_name_arn" {
  default = "The arn suffix of the aae subscription key stored in secrets manager"
}