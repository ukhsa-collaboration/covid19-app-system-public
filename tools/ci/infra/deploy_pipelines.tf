module "artifacts" {
  source = "./libraries/artifacts_s3"
  name   = "${var.account}-build-artifacts-archive"
}

data "aws_secretsmanager_secret_version" "github" {
  secret_id = var.github_credentials
}

module "app_system_deployment" {
  for_each                 = toset(var.target_environments)
  source                   = "./modules/github_codebuild"
  name                     = "deploy-cta-${each.key}"
  account                  = var.account
  tags                     = var.tags
  repository               = var.repository_url
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/deploy.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}


module "tier_metadata" {
  for_each                 = toset(var.target_environments)
  source                   = "./modules/github_codebuild"
  name                     = "deploy-tier-metadata-${each.key}"
  account                  = var.account
  tags                     = var.tags
  repository               = var.repository_url
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/deploy-tier-metadata.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}

module "local_messages" {
  for_each                 = toset(var.target_environments)
  source                   = "./modules/github_codebuild"
  name                     = "deploy-local-messages-${each.key}"
  account                  = var.account
  tags                     = var.tags
  repository               = var.repository_url
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/deploy-local-messages.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}

module "analytics_deployment" {
  for_each                 = toset(var.target_environments)
  source                   = "./modules/github_codebuild"
  name                     = "deploy-analytics-${each.key}"
  account                  = var.account
  tags                     = var.tags
  repository               = var.repository_url
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/deploy-analytics.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}

module "pubdash_deployment" {
  for_each                 = toset(var.target_environments)
  source                   = "./modules/github_codebuild"
  name                     = "deploy-pubdash-${each.key}"
  account                  = var.account
  tags                     = var.tags
  repository               = var.repository_url
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/deploy-pubdash.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}
