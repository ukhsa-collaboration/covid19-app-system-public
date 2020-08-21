locals {
  function_name          = "${terraform.workspace}-${var.name}"
  dependency_anchor_char = length(var.dependency_ref) > 0 ? substr(var.dependency_ref, 0, 1) : "a"
  vars = {
    base_domain = var.base_domain
    hostname    = var.hostname
    uri_path    = var.uri_path
    method      = var.method
    auth_header = var.auth_header
    expc_status = var.expc_status
  }
  lambda_source = templatefile(var.synthetic_script_path, local.vars)
}

resource "random_string" "force_new_lambda_file" {
  length  = 7
  upper   = false
  lower   = true
  number  = true
  special = false
}

data "archive_file" "lambda_exporter" {
  type        = "zip"
  output_path = "${path.root}/../../../../out/gen/synthetics/${var.name}-${random_string.force_new_lambda_file.result}${local.dependency_anchor_char}.zip"
  source {
    content  = local.lambda_source
    filename = "nodejs/node_modules/canary.js"
  }
}

resource "synthetics_canary" "watchdog" {
  name                 = local.function_name
  execution_role_arn   = var.lambda_exec_role_arn
  artifact_s3_location = "s3://${var.artifact_s3_bucket}/${local.function_name}"
  zip_file             = data.archive_file.lambda_exporter.output_path
  handler              = "canary.handler"
  vpc_config {
    security_group_ids = var.synthetic_vpc_config.security_group_ids
    subnet_ids         = var.synthetic_vpc_config.subnet_ids
  }
  schedule {
    duration_in_seconds = var.synthetic_schedule.duration_in_seconds
    expression          = var.synthetic_schedule.expression
  }
}
