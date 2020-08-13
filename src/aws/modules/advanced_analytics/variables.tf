variable name {
  description = "The name of the lambda function"
}

variable lambda_timeout {
  description = "The timeout for the lambda execution"
}

variable lambda_handler {
  description = "The name for the lambda entry point (handler)"
}

variable analytics_submission_store {
  description = "The name of the analytics submission store"
}

variable aae_environment {
  description = "The name of the AAE endpoint base domain in DNS (set in main.tf in the appropriate src/aws/accounts subdirectory)"
}