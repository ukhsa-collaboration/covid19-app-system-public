module "app_system_pull_request" {
  count                    = var.allow_dev_pipelines == true ? 1 : 0
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

module "app_system_ci" {
  count                    = var.allow_dev_pipelines == true ? 1 : 0
  source                   = "./modules/github_ci_codebuild"
  name                     = "ci-app-system"
  account                  = var.account
  tags                     = var.tags
  repository               = "https://github.com/nhsx/covid19-app-system-public.git"
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/continuous-integration.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}

module "resources_cleanup" {
  count                    = var.allow_dev_pipelines == true ? 1 : 0
  source                   = "./modules/github_resources_cleanup_codebuild"
  name                     = "resources-cleanup"
  account                  = var.account
  tags                     = var.tags
  repository               = "https://github.com/nhsx/covid19-app-system-public.git"
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/cleanup.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}
