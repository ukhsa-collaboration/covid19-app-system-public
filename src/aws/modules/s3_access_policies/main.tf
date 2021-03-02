data "aws_iam_policy_document" "base_policy" {
  statement {
    actions = ["s3:*"]
    principals {
      type        = "AWS"
      identifiers = ["*"]
    }
    resources = ["${var.s3_bucket_arn}/*"]

    effect = "Deny"

    condition {
      test     = "Bool"
      values   = ["false"]
      variable = "aws:SecureTransport"
    }
  }
}

data "aws_iam_policy_document" "origin_access_policy" {
  source_json = data.aws_iam_policy_document.base_policy.json
  statement {
    actions = ["s3:GetObject"]
    principals {
      type        = "AWS"
      identifiers = [var.origin_access_identity_arn]
    }
    resources = ["${var.s3_bucket_arn}/*"]
  }
}

data "aws_iam_policy_document" "cross_account_s3_readonly_policy" {
  source_json = (
    var.origin_access_identity_arn == ""
    ? data.aws_iam_policy_document.base_policy.json
    : data.aws_iam_policy_document.origin_access_policy.json
  )
  statement {
    sid = "${var.prefix}CrossAccountReadonlyAccess"
    actions = [
      "s3:GetBucketLocation",
      "s3:GetObject",
      "s3:ListBucket",
      "s3:ListBucketMultipartUploads",
      "s3:ListMultipartUploadParts",
    ]
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = formatlist("arn:aws:iam::%s:root", var.principal_aws_accounts)
    }
    resources = [
      var.s3_bucket_arn,
      "${var.s3_bucket_arn}/*",
    ]
  }
}

locals {
  policies = {
    "default" : data.aws_iam_policy_document.base_policy
    "secure_origin_access" : data.aws_iam_policy_document.origin_access_policy
    "cross_account_readonly" : data.aws_iam_policy_document.cross_account_s3_readonly_policy
  }
  policy_document = lookup(local.policies, var.policy_type, data.aws_iam_policy_document.base_policy)
}
