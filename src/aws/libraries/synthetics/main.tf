locals {
  vars = {
    base_domain = var.base_domain
    hostname    = var.hostname
    uri_path    = var.uri_path
    method      = var.method
    secret_name = var.secret_name
    expc_status = var.expc_status
  }
  lambda_source = templatefile(var.synthetic_script_path, local.vars)
  function_name = "${terraform.workspace}-${var.name}"
}

// There appears to be no way to make the archive_file resource realise that
// the templated source code has changed.
// So we just delete the zip file to force archive_file to re-create it.
resource "null_resource" "canary_changed" {
  triggers = {
    // The first two triggers don't actually change, they are merely here to let
    // other resources further down the chain depend on the null_resource.
    function_name    = local.function_name
    lambda_path      = "${path.root}/../../../../out/gen/synthetics/${local.function_name}/canary.zip"
    source_code_hash = base64sha256(local.lambda_source)
  }

  provisioner "local-exec" {
    command = "rm -f ${null_resource.canary_changed.triggers.lambda_path}"
  }
}

data "archive_file" "lambda_exporter" {
  type        = "zip"
  output_path = "${null_resource.canary_changed.triggers.lambda_path}"
  source {
    content  = local.lambda_source
    filename = "nodejs/node_modules/canary.js"
  }
}

// Unfortunately the synthetics_canary resource, unlike the lambda resource,
// does not have a hash-checksum property. We use this null resource to force
// the synthetics_canary resource to depend on the zip file change.
resource "null_resource" "zip_file_changed" {
  triggers = {
    checksum = data.archive_file.lambda_exporter.output_base64sha256
    // The next two triggers don't ever change, but are here to allow dependency
    // on this resource by the synthetics_canary resource
    zip_file      = data.archive_file.lambda_exporter.output_path
    function_name = null_resource.canary_changed.triggers.function_name
  }
}

resource "synthetics_canary" "watchdog" {
  count                = var.enabled ? 1 : 0
  name                 = null_resource.zip_file_changed.triggers.function_name
  execution_role_arn   = var.lambda_exec_role_arn
  artifact_s3_location = "s3://${var.artifact_s3_bucket}/${null_resource.zip_file_changed.triggers.function_name}"
  zip_file             = null_resource.zip_file_changed.triggers.zip_file
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
