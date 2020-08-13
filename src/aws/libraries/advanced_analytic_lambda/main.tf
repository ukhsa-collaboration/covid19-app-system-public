locals {
  secret_name_prefix = "/aae/dev"
  lambda_path        = "${path.module}/../../lambdas/advanced_analytics/advancedAnalytics.py"
  vars = {
    certificate_secret_name         = var.secret_names.private_certificate
    key_secret_name                 = var.secret_names.private_key
    encryption_password_secret_name = var.secret_names.certificate_encryption_password
    subscription_key_name           = var.secret_names.subscription_key
    aae_environment                 = var.aae_environment
  }
  lambda_source = templatefile(local.lambda_path, local.vars)
}

data "archive_file" "this" {
  type = "zip"
  source {
    content  = local.lambda_source
    filename = basename(local.lambda_path)
  }
  output_path = "${path.root}/../../../../out/python/${var.name}.zip"
}

resource "aws_cloudwatch_log_group" "this" {
  name = "/aws/lambda/${var.name}"
}

resource "aws_lambda_function" "this" {
  function_name    = var.name
  runtime          = "python3.8"
  timeout          = var.lambda_timeout
  handler          = var.lambda_handler
  role             = var.iam_advanced_analytics_lambda_arn
  filename         = data.archive_file.this.output_path
  source_code_hash = data.archive_file.this.output_base64sha256
  depends_on       = [aws_cloudwatch_log_group.this]

  tracing_config {
    mode = "Active"
  }
}

resource "aws_lambda_permission" "to_execute" {
  statement_id  = "AllowExecutionFromS3Bucket"
  action        = "lambda:InvokeFunction"
  function_name = var.name
  principal     = "s3.amazonaws.com"
  source_arn    = "arn:aws:s3:::${var.analytics_submission_store}"

  depends_on = [aws_lambda_function.this]
}

# Attach event notification on analytics_submission bucket.
resource "aws_s3_bucket_notification" "bucket_notification" {
  bucket = var.analytics_submission_store
  lambda_function {
    lambda_function_arn = aws_lambda_function.this.arn
    events              = ["s3:ObjectCreated:*"]
    filter_suffix       = ".json"
  }
  depends_on = [aws_lambda_permission.to_execute]
}