variable "name" {
  description = "Name of the CloudFront distribution"
}

variable "exposure_configuration_bucket_regional_domain_name" {
  description = "The S3 bucket regional domain name used to construct the URLs for exposure configuration data"
}

variable "exposure_configuration_payload" {
  description = "The path (i.e. route) to the exposure configuration payload"
}

variable "exposure_configuration_origin_access_identity_path" {
  description = "The origin access controlling access to the store"
}

variable "risky_post_district_distribution_bucket_regional_domain_name" {
  description = "The S3 bucket regional domain name used to construct the URLs for risky post districts data"
}

variable "risky_post_district_payload" {
  description = "The path (i.e. route) to the post districts payload"
}

variable "risky_post_district_v2_payload" {
  description = "The path (i.e. route) to the post districts v2 payload"
}

variable "risky_post_district_origin_access_identity_path" {
  description = "The origin access controlling access to the store"
}

variable "risky_venues_bucket_regional_domain_name" {
  description = "The S3 bucket regional domain name used to construct the URLs for risky venues data"
}

variable "risky_venues_payload" {
  description = "The path (i.e. route) to the venues payload"
}

variable "risky_venues_origin_access_identity_path" {
  description = "The origin access controlling access to the store"
}

variable "self_isolation_bucket_regional_domain_name" {
  description = "The S3 bucket regional domain name used to construct the URLs for self isolation configuration data"
}

variable "self_isolation_payload" {
  description = "The path (i.e. route) to the self isolation configuration payload"
}

variable "self_isolation_origin_access_identity_path" {
  description = "The origin access controlling access to the store"
}

variable "symptomatic_questionnaire_bucket_regional_domain_name" {
  description = "The S3 bucket regional domain name used to construct the URLs for symptomatic questionnaire configuration data"
}

variable "symptomatic_questionnaire_payload" {
  description = "The path (i.e. route) to the symptomatic questionnaire configuration payload"
}

variable "symptomatic_questionnaire_origin_access_identity_path" {
  description = "The origin access controlling access to the store"
}

variable "diagnosis_keys_bucket_regional_domain_name" {

}

variable "diagnosis_keys_origin_access_identity_path" {

}

variable "diagnosis_keys_path_2hourly" {

}

variable "diagnosis_keys_path_daily" {

}

variable "availability_android_bucket_regional_domain_name" {
  description = "The S3 bucket regional domain name used to construct the URLs"
}

variable "availability_android_payload" {
  description = "The path (i.e. route) to the payload"
}

variable "availability_android_origin_access_identity_path" {
  description = "The origin access controlling access to the store"
}

variable "availability_ios_bucket_regional_domain_name" {
  description = "The S3 bucket regional domain name used to construct the URLs"
}

variable "availability_ios_payload" {
  description = "The path (i.e. route) to the payload"
}

variable "availability_ios_origin_access_identity_path" {
  description = "The origin access controlling access to the store"
}

variable "domain" {
  description = "The domain the CloudFront distribution needs to be deployed into"
}

variable "web_acl_arn" {
  description = "The ARN of the WAFv2 web acl to filter CloudFront requests"
}

variable "enable_shield_protection" {
  description = "Flag to enable/disable shield protection"
  type        = bool
}
