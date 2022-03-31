module "app_system_pull_request" {
  count                       = var.allow_dev_pipelines == true ? 1 : 0
  source                      = "./modules/github_pr_codebuild"
  name                        = "pr-app-system"
  account                     = var.account
  tags                        = var.tags
  repository                  = var.repository_url
  container                   = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name       = module.artifacts.bucket_name
  cache_artifacts_bucket_name = module.cache_artifacts.bucket_name
  pipeline_definition_file    = abspath("${path.root}/../../../pipelines/pr.app-system.buildspec.yml")
  service_role                = var.service_role
  image_pull_credentials_type = "SERVICE_ROLE"
  github_api_token            = data.aws_secretsmanager_secret_version.github.secret_string
}

module "app_system_ci" {
  count                       = var.allow_dev_pipelines == true ? 1 : 0
  source                      = "./modules/github_ci_codebuild"
  name                        = "ci-app-system"
  account                     = var.account
  tags                        = var.tags
  repository                  = var.repository_url
  container                   = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name       = module.artifacts.bucket_name
  cache_artifacts_bucket_name = module.cache_artifacts.bucket_name
  pipeline_definition_file    = abspath("${path.root}/../../../pipelines/continuous-integration.buildspec.yml")
  service_role                = var.service_role
  image_pull_credentials_type = "SERVICE_ROLE"
  github_api_token            = data.aws_secretsmanager_secret_version.github.secret_string
}

module "doreto_pull_request" {
  count                       = var.allow_dev_pipelines == true ? 1 : 0
  source                      = "./modules/github_pr_codebuild"
  name                        = "pr-doreto"
  account                     = var.account
  tags                        = var.tags
  repository                  = var.repository_url
  container                   = "aws/codebuild/standard:5.0"
  artifacts_bucket_name       = module.artifacts.bucket_name
  cache_artifacts_bucket_name = module.cache_artifacts.bucket_name
  pipeline_definition_file    = abspath("${path.root}/../../../pipelines/pr.doreto.buildspec.yml")
  service_role                = var.service_role
  image_pull_credentials_type = "CODEBUILD"
  privileged_mode             = true
  github_api_token            = data.aws_secretsmanager_secret_version.github.secret_string
  file_path                   = "src/documentation_reporting_tool/"
}

module "devenv_ci" {
  count                       = var.allow_dev_pipelines == true ? 1 : 0
  source                      = "./modules/github_ci_codebuild"
  name                        = "ci-devenv"
  account                     = var.account
  tags                        = var.tags
  repository                  = var.repository_url
  container                   = "aws/codebuild/standard:5.0"
  artifacts_bucket_name       = module.artifacts.bucket_name
  cache_artifacts_bucket_name = module.cache_artifacts.bucket_name
  pipeline_definition_file    = abspath("${path.root}/../../../pipelines/devenv.buildspec.yml")
  service_role                = var.service_role
  image_pull_credentials_type = "CODEBUILD"
  privileged_mode             = true
  github_api_token            = data.aws_secretsmanager_secret_version.github.secret_string
}

module "resources_cleanup" {
  count                    = var.allow_dev_pipelines == true ? 1 : 0
  source                   = "./modules/github_resources_cleanup_codebuild"
  name                     = "resources-cleanup"
  account                  = var.account
  tags                     = var.tags
  repository               = var.repository_url
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/cleanup.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}

output "pr_webhook_app_system" {
  value = module.app_system_pull_request.*.webhook_url
}

output "pr_webhook_doreto" {
  value = module.doreto_pull_request.*.webhook_url
}

output "ci_webhook_app_system" {
  value = module.app_system_ci.*.webhook_url
}

output "devenv_webhook_app_system" {
  value = module.devenv_ci.*.webhook_url
}
