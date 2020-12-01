variable "isolation_payment_website" {
  description = "The website for claiming isolation payments"
}

variable "isolation_token_expiry_in_weeks" {
  description = "The time to live for the isolation token in weeks"
}

variable "isolation_payment_trust_mappings" {
  description = "value: principals which have access to Isolation Payment verify- and consume Lambdas. key: target environment"
  type        = map(list(string))
}

variable "isolation_payment_countries_whitelisted" {
  description = "The countries whitelisted for isolation payment"
}

variable "rate_limit" {
  description = "The requests per second limit for this API gateway"
}

variable "burst_limit" {
  description = "The requests per second burst limit for this API gateway"
}

variable "lambda_repository_bucket" {
  description = "The name of the bucket which contains the lambda jar"
}

variable "lambda_object_key" {
  description = "The object key of the lambda jar in the s3 bucket"
}

variable "custom_oai" {
  description = "Secret shared between CloudFront Distribution and Lambda"
}

variable "alarm_topic_arn" {
  description = "SNS topic to publish application metric alarms to"
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "isolation_payment_token_creation_enabled" {
  description = "Feature flag to enable/disable token creation/update endpoints"
}
