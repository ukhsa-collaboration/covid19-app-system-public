locals {
  identifier_prefix = "${terraform.workspace}-${var.lambda_project}-artifact-repository"
  service           = "repository"
  lambda_source     = var.lambda_zip_path
  lambda_hash       = filemd5(local.lambda_source)
  lambda_key        = "lambda/${var.lambda_project}-${local.lambda_hash}.zip"
}

resource "aws_s3_bucket_metric" "bucket_request_metrics" {
  bucket = aws_s3_bucket.this.bucket
  name   = aws_s3_bucket.this.bucket
}

resource "aws_s3_bucket" "this" {
  bucket = local.identifier_prefix
  acl    = "private"

  force_destroy = var.force_destroy_s3_buckets

  tags = var.tags

  versioning {
    enabled = false # *** PRIVACY / AG Terms & Conditions (CHT) *** Make sure versioning __is disabled__ because we store diagnosis keys in these buckets !!!
  }

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }

  logging {
    target_bucket = var.logs_bucket_id
    target_prefix = "${local.identifier_prefix}/"
  }
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_object" "lambda" {
  bucket = aws_s3_bucket.this.id
  key    = local.lambda_key
  source = local.lambda_source
  etag   = local.lambda_hash
}

resource "aws_s3_bucket_policy" "this" {
  depends_on = [aws_s3_bucket_public_access_block.this] # in terraform v0.12.29 we encounter conflict when this is executed concurrently with setting public access block
  bucket     = aws_s3_bucket.this.id
  policy     = var.policy_document.json
}
