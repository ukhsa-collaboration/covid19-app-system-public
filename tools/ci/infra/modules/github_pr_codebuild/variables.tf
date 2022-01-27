variable "account" {
  description = "The name of the account hosting the pipeline"
  type        = string
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
  default     = {}
}

variable "name" {
  description = " The name of the pipeline"
  type        = string
}

variable "repository" {
  description = "URL to the repository used in the pipeline build steps"
  type        = string
}

variable "pipeline_definition_file" {
  description = "The path to the build definition YAML relative to the root of the repository"
  type        = string
}

variable "container" {
  description = "The docker image to use as the build & deploy environment"
  type        = string
}

variable "artifacts_bucket_name" {
  description = "The name of the S3 bucket to store the build artifacts in"
  type        = string
}

variable "service_role" {
  description = "The ARN of the service role used to run the builds"
  type        = string
}

variable "image_pull_credentials_type" {
  description = "The type of credentials AWS CodeBuild uses to pull images"
  type        = string
}

variable "privileged_mode" {
  description = "Whether to enable running the Docker daemon inside a Docker container"
  type        = bool
  default     = false
}

variable "github_api_token" {
  description = "The authentication token for GitHub"
  type        = string
  default     = "FIXME"
}

variable "file_path" {
  description = "The regular expression to match against file path in the source to run codebuild jobs for"
  type        = string
  default     = ""
}
