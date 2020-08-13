locals {
  identifier_prefix = "${terraform.workspace}-${var.name}-${var.service}"
}

resource "aws_s3_bucket" "this" {
  bucket = local.identifier_prefix
  acl    = "private"
  policy = ""

  #FIXME make this environment-specific (false for prod, true for non-prod - especially for short living environments for git branches)
  force_destroy = true

  tags = {
    Environment = terraform.workspace
    Service     = var.service
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
    actions = ["s3:GetObject"]
    principals {
      type        = "AWS"
      identifiers = [var.origin_access_identity_path]
    }
    resources = ["${aws_s3_bucket.this.arn}/*"]
  }

  statement {
    actions = ["s3:GetObject"]
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
  bucket = aws_s3_bucket.this.id
  policy = data.aws_iam_policy_document.this.json
}

