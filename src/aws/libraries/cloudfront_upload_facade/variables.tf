variable "name" {
  description = "Name of the CloudFront distribution"
}

variable "risky-post-districts-upload-endpoint" {
  description = "The endpoint for risky post districts upload"
}

variable "risky-post-districts-upload-path" {
  description = "The route to the risky post districts upload"
}

variable "risky-venues-upload-endpoint" {
  description = "The endpoint for risky venues upload"
}

variable "risky-venues-upload-path" {
  description = "The route to the risky post districts upload"
}

variable "test-results-upload-endpoint" {
  description = "The endpoint for test results upload"
}

variable "test-results-upload-path" {
  description = "The route to the test results upload"
}

variable "domain" {
  description = "The domain the CloudFront distribution needs to be deployed into"
}

variable "web_acl_arn" {
  description = "The ARN of the WAFv2 web acl to filter CloudFront requests"
}
