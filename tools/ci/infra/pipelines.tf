
module "artifacts" {
  source = "./libraries/artifacts_s3"
  name   = "${terraform.workspace}-build-artifacts"
}

data "aws_secretsmanager_secret_version" "github" {
  secret_id = var.github_credentials
}

module "app_system_deployment" {
  source                   = "./modules/github_codebuild"
  name                     = "deploy-app-system"
  account                  = var.account
  tags                     = var.tags
  repository               = "https://github.com/nhsx/covid19-app-system-public.git"
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/deploy.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}
module "app_system_pull_request" {
  source                   = "./modules/github_pr_codebuild"
  name                     = "pr-app-system"
  account                  = var.account
  tags                     = var.tags
  repository               = "https://github.com/nhsx/covid19-app-system-public.git"
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/pr.app-system.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}

module "tier_metadata" {
  source                   = "./modules/github_codebuild"
  name                     = "deploy-tier-metadata"
  account                  = var.account
  tags                     = var.tags
  repository               = "https://github.com/nhsx/covid19-app-system-public.git"
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/deploy-tier-metadata.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}

output "pullrequest_webhook_app_system" {
  value = module.app_system_pull_request.webhook_url
}

module "analytics_deployment" {
  source                   = "./modules/github_codebuild"
  name                     = "deploy-analytics"
  account                  = var.account
  tags                     = var.tags
  repository               = "https://github.com/nhsx/covid19-app-system-public.git"
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/deploy-analytics.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}
