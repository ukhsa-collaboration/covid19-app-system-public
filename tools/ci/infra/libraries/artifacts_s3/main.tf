locals {
  identifier_prefix = "${terraform.workspace}-${var.name}"
}

resource "aws_s3_bucket_metric" "bucket_request_metrics" {
  bucket = aws_s3_bucket.this.bucket
  name   = aws_s3_bucket.this.bucket
}

resource "aws_s3_bucket" "this" {
  bucket = var.name
  acl    = "private"

  force_destroy = true

  tags = var.tags

  versioning {
    enabled = false
  }

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }

  website {
    index_document = "index.html"
    error_document = "error.html"
  }

  dynamic "lifecycle_rule" {
    for_each = var.lifecycle_rules
    content {
      id                                     = lifecycle_rule.value.id
      prefix                                 = lifecycle_rule.value.prefix
      enabled                                = lifecycle_rule.value.enabled
      abort_incomplete_multipart_upload_days = 1

      expiration {
        days = lifecycle_rule.value.days
      }
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
