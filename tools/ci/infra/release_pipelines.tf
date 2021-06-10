module "cta_release" {
  count                    = var.allow_prod_pipelines == true ? 1 : 0
  source                   = "./modules/github_codebuild"
  name                     = "release-cta-${var.account}"
  account                  = var.account
  tags                     = var.tags
  repository               = var.repository_url
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/release.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}

module "tier_metadata_release" {
  count                    = var.allow_prod_pipelines == true ? 1 : 0
  source                   = "./modules/github_codebuild"
  name                     = "release-tier-metadata-${var.account}"
  account                  = var.account
  tags                     = var.tags
  repository               = var.repository_url
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/release-tier-metadata.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}

module "availability_release" {
  count                    = var.allow_prod_pipelines == true ? 1 : 0
  source                   = "./modules/github_codebuild"
  name                     = "release-availability-${var.account}"
  account                  = var.account
  tags                     = var.tags
  repository               = var.repository_url
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/release-availability.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}

module "local_messages_release" {
  count                    = var.allow_prod_pipelines == true ? 1 : 0
  source                   = "./modules/github_codebuild"
  name                     = "release-local-messages-${var.account}"
  account                  = var.account
  tags                     = var.tags
  repository               = var.repository_url
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/release-local-messages.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}

module "analytics_release" {
  count                    = var.allow_prod_pipelines == true ? 1 : 0
  source                   = "./modules/github_codebuild"
  name                     = "release-analytics-${var.account}"
  account                  = var.account
  tags                     = var.tags
  repository               = var.repository_url
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/release-analytics.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}

module "pubdash_release" {
  count                    = var.allow_prod_pipelines == true ? 1 : 0
  source                   = "./modules/github_codebuild"
  name                     = "release-pubdash-${var.account}"
  account                  = var.account
  tags                     = var.tags
  repository               = var.repository_url
  container                = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv-latest"
  artifacts_bucket_name    = module.artifacts.bucket_name
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/release-pubdash.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}
