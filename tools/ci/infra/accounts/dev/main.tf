module "app-system-ci" {
  source = "../.."
  tags = {
    Environment = "Dev"
    Owner       = "Zuhlke"
    Application = "TestTraceAppCI"
    Criticality = "Tier 3"
    Revision    = var.ci-infra-revision
  }
  service_role                 = "arn:aws:iam::123456789012:role/dev-ApplicationDeploymentCodeBuild"
  build_failure_events_sns_arn = data.terraform_remote_state.app_services_core_infra.outputs.cicd_build_events_sns_arn
  deploy_events_sns_arn        = data.terraform_remote_state.app_services_core_infra.outputs.cicd_deploy_events_sns_arn
  account                      = "dev"
  # Secrets manager entry containing the GitHub API token
  github_credentials  = "/ci/github"
  target_environments = var.target_environments
  allow_dev_pipelines = true
  repository_url      = "https://github.com/nihp-public/covid19-app-system-public.git"
}
