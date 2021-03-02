locals {
  identifier_prefix = "${terraform.workspace}-${var.name}-${var.service}"
}

resource "aws_s3_bucket" "this" {
  bucket = local.identifier_prefix
  acl    = "private"
  policy = ""

  force_destroy = var.force_destroy_s3_buckets

  tags = var.tags

  versioning {
    enabled = var.s3_versioning
  }

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
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
  depends_on = [aws_s3_bucket_public_access_block.this] # in terraform v0.12.29 we encounter conflict when this is executed concurrently with setting public access block
  bucket     = aws_s3_bucket.this.id
  policy     = var.policy_document.json
}
