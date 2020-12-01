
module "artifacts" {
  source = "./libraries/artifacts_s3"
  name   = "build-artifacts"
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
  artifacts_bucket_arn     = module.artifacts.bucket_arn
  pipeline_definition_file = abspath("${path.root}/../../../pipelines/deploy.buildspec.yml")
  service_role             = var.service_role
  github_api_token         = data.aws_secretsmanager_secret_version.github.secret_string
}
