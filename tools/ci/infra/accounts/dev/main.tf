module "app-system-ci" {
  source = "../.."
  tags = {
    Environment = "Dev"
    Owner       = "Zuhlke"
    Application = "TestTraceAppCI"
    Criticality = "Tier 3"
  }
  service_role = "arn:aws:iam::123456789012:role/dev-ApplicationDeploymentCodeBuild"
  account      = "dev"
  # Secrets manager entry containing the GitHub API token
  github_credentials = "/ci/github"
}
