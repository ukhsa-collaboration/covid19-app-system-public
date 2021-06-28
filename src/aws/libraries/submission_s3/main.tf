locals {
  identifier_prefix = "${terraform.workspace}-${var.name}-${var.service}"
  replicator        = var.replication_enabled ? [true] : []
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

  dynamic "logging" {
    for_each = var.logs_bucket_id == null || var.logs_bucket_id == "" ? [] : [{}]
    content {
      target_bucket = var.logs_bucket_id
      target_prefix = "${local.identifier_prefix}/"
    }
  }
  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }
  lifecycle_rule {
    id      = "${local.identifier_prefix}-days_to_retain"
    enabled = var.lifecycle_rule_enabled
    expiration {
      days = var.days_to_live
    }
    noncurrent_version_expiration {
      days = var.days_to_live
    }
  }

  # Replication relevant settings. These are activated via var.replication_enabled and are
  # only active on staging and prod
  versioning {
    enabled = var.replication_enabled
  }

  dynamic "replication_configuration" {
    for_each = local.replicator
    content {
      role = aws_iam_role.replication[0].arn
      rules {
        id     = "${local.identifier_prefix}-replication-config"
        status = "Enabled"

        destination {
          bucket        = aws_s3_bucket.destination[0].arn
          storage_class = "STANDARD"
        }
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

resource "aws_s3_bucket_policy" "this" {
  # in terraform v0.12.29 we encounter conflict when this is executed concurrently with setting public access block
  depends_on = [aws_s3_bucket_public_access_block.this]
  bucket     = aws_s3_bucket.this.id
  policy     = var.policy_document.json
}

resource "aws_s3_bucket_metric" "bucket_request_metrics_destination" {
  count  = var.replication_enabled ? 1 : 0
  bucket = aws_s3_bucket.destination[0].bucket
  name   = aws_s3_bucket.destination[0].bucket
}

# Replication relevant settings. These are activated via var.replication_enabled and are
# only active on staging and prod

resource "aws_s3_bucket" "destination" {
  count  = var.replication_enabled ? 1 : 0
  bucket = "${local.identifier_prefix}-replica"

  force_destroy = var.force_destroy_s3_buckets

  tags = var.tags
  dynamic "logging" {
    for_each = var.logs_bucket_id == null || var.logs_bucket_id == "" ? [] : [{}]
    content {
      target_bucket = var.logs_bucket_id
      target_prefix = "${local.identifier_prefix}/"
    }
  }
  lifecycle_rule {
    id      = "${local.identifier_prefix}-replica-days_to_retain"
    enabled = var.lifecycle_rule_enabled
    expiration {
      days = var.days_to_live
    }
    noncurrent_version_expiration {
      days = var.days_to_live
    }
  }
  versioning {
    enabled = true
  }
  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }
}

resource "aws_s3_bucket_public_access_block" "replica" {
  count  = var.replication_enabled ? 1 : 0
  bucket = aws_s3_bucket.destination[0].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

data "aws_iam_policy_document" "replica" {
  count = var.replication_enabled ? 1 : 0
  statement {
    actions = ["s3:*"]
    principals {
      type        = "AWS"
      identifiers = ["*"]
    }
    resources = ["${aws_s3_bucket.destination[0].arn}/*"]

    effect = "Deny"

    condition {
      test     = "Bool"
      values   = ["false"]
      variable = "aws:SecureTransport"
    }
  }
}

resource "aws_s3_bucket_policy" "replica" {
  # in terraform v0.12.29 we encounter conflict when this is executed concurrently with setting public access block
  depends_on = [aws_s3_bucket_public_access_block.replica]
  count      = var.replication_enabled ? 1 : 0
  bucket     = aws_s3_bucket.destination[0].id
  policy     = data.aws_iam_policy_document.replica[0].json
}

resource "aws_iam_role" "replication" {
  count = var.replication_enabled ? 1 : 0
  name  = "${terraform.workspace}-${var.name}-replication"

  tags = var.tags

  assume_role_policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "s3.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
POLICY
}

resource "aws_iam_policy" "replication" {
  count = var.replication_enabled ? 1 : 0
  name  = "${terraform.workspace}-${var.name}-replication"

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "s3:GetReplicationConfiguration",
        "s3:ListBucket"
      ],
      "Effect": "Allow",
      "Resource": [
        "${aws_s3_bucket.this.arn}"
      ]
    },
    {
      "Action": [
        "s3:GetObjectVersion",
        "s3:GetObjectVersionAcl"
      ],
      "Effect": "Allow",
      "Resource": [
        "${aws_s3_bucket.this.arn}/*"
      ]
    },
    {
      "Action": [
        "s3:ReplicateObject",
        "s3:ReplicateDelete"
      ],
      "Effect": "Allow",
      "Resource": "${aws_s3_bucket.destination[0].arn}/*"
    }
  ]
}
POLICY
}

resource "aws_iam_role_policy_attachment" "replication" {
  count      = var.replication_enabled ? 1 : 0
  role       = aws_iam_role.replication[0].name
  policy_arn = aws_iam_policy.replication[0].arn
}
