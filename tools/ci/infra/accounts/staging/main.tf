module "app-system-ci" {
  source = "../.."
  tags = {
    Environment = "Dev"
    Owner       = "Zuhlke"
    Application = "TestTraceAppCI"
    Criticality = "Tier 3"
    Revision    = var.ci-infra-revision
  }
  service_role = "arn:aws:iam::123456789012:role/staging-ApplicationDeploymentCodeBuild"
  account      = "staging"
  # Secrets manager entry containing the GitHub API token
  github_credentials  = "/ci/github"
  target_environments = var.target_environments
}
