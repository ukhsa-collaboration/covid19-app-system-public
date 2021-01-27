variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "service_role" {
  description = "The ARN of the service role used to run the builds"
  type        = string
}

variable "account" {
  description = "The name of the account hosting the pipeline"
  type        = string
}

variable "github_credentials" {
  description = "The secrets manager entry containing the GitHub API token"
  type        = string
}
variable "target_environments" {
  description = "the list of target environments hosted in this account"
}
