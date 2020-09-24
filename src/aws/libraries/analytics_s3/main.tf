# Creates a bucket with CORS settings

locals {
  identifier_prefix = "${terraform.workspace}-${var.name}-${var.service}"
}

resource "aws_s3_bucket" "this" {
  bucket = local.identifier_prefix
  acl    = "private"

  force_destroy = var.force_destroy_s3_buckets

  tags = {
    Environment = terraform.workspace
    Service     = var.service
  }

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "HEAD"]
    expose_headers  = ["x-amz-server-side-encryption", "x-amz-request-id", "x-amz-id-2", "ETag"]
    max_age_seconds = 3000
    allowed_origins = ["*"]
  }

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

  lifecycle_rule {
    enabled = true
    id      = "${local.identifier_prefix}-clean-up"
    prefix  = "public/query-results/"
    expiration {
      days = 5
    }
  }
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

data "aws_iam_policy_document" "this" {
  statement {
    actions = ["s3:*"]
    principals {
      type        = "AWS"
      identifiers = ["*"]
    }
    resources = ["${aws_s3_bucket.this.arn}/*"]

    effect = "Deny"

    condition {
      test     = "Bool"
      values   = ["false"]
      variable = "aws:SecureTransport"
    }
  }
}

resource "aws_s3_bucket_policy" "this" {
  depends_on = [aws_s3_bucket_public_access_block.this] # in terraform v0.12.29 we encounter conflict when this is executed concurrently with setting public access block
  bucket     = aws_s3_bucket.this.id
  policy     = data.aws_iam_policy_document.this.json
}
