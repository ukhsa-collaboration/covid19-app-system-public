locals {
  identifier_prefix = "${terraform.workspace}-${var.name}"
}

resource "aws_codebuild_source_credential" "this" {
  auth_type   = "PERSONAL_ACCESS_TOKEN"
  server_type = "GITHUB"
  token       = var.github_api_token
}

data "template_file" "buildspec" {
  template = file(var.pipeline_definition_file)
}

resource "aws_codebuild_project" "this" {
  name           = var.name
  description    = "Deployment pipeline"
  build_timeout  = "180"
  service_role   = var.service_role
  source_version = "master"

  tags = var.tags

  artifacts {
    type                   = "S3"
    location               = var.artifacts_bucket_name
    packaging              = "ZIP"
    override_artifact_name = true
  }

  environment {
    compute_type                = "BUILD_GENERAL1_MEDIUM"
    image                       = var.container
    type                        = "LINUX_CONTAINER"
    image_pull_credentials_type = "SERVICE_ROLE"

    environment_variable {
      name  = "ACCOUNT"
      value = var.account
    }
  }

  logs_config {
    cloudwatch_logs {
      group_name  = "${local.identifier_prefix}-codebuild"
      stream_name = "${local.identifier_prefix}-codebuild"
    }
  }

  source {
    buildspec = data.template_file.buildspec.rendered
    type      = "GITHUB"
    location  = var.repository

    git_submodules_config {
      fetch_submodules = false
    }
    auth {
      type     = "OAUTH"
      resource = aws_codebuild_source_credential.this.arn
    }
  }
}

# Run at 1 AM UTC everyday
resource "aws_cloudwatch_event_rule" "every_week_day" {
  name                = "${local.identifier_prefix}-every-week-day"
  schedule_expression = "cron(00 1 ? * MON,TUE,WED,THU,FRI *)"
}

resource "aws_cloudwatch_event_target" "target_codebuild_project" {
  rule     = aws_cloudwatch_event_rule.every_week_day.name
  arn      = aws_codebuild_project.this.arn
  role_arn = var.service_role
}
