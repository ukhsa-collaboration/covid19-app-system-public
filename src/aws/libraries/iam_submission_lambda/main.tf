locals {
  lambda_policies = [
    "arn:aws:iam::aws:policy/AmazonS3FullAccess",
    "arn:aws:iam::aws:policy/CloudWatchFullAccess",
    "arn:aws:iam::aws:policy/AWSXrayFullAccess",
    "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess",
    "arn:aws:iam::aws:policy/AmazonSSMReadOnlyAccess",
    "arn:aws:iam::aws:policy/SecretsManagerReadWrite",
    "arn:aws:iam::aws:policy/CloudFrontFullAccess",
    "arn:aws:iam::aws:policy/AmazonKinesisFirehoseFullAccess"
  ]
}

data "aws_iam_policy_document" "this" {
  statement {
    actions = [
      "kms:Sign"
    ]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "this" {
  name_prefix = "${var.name}-lambda"
  path        = "/"
  policy      = data.aws_iam_policy_document.this.json
}

resource "aws_iam_role_policy_attachment" "this" {
  role       = aws_iam_role.this.name
  policy_arn = aws_iam_policy.this.arn
}

resource "aws_iam_role" "this" {
  name = "${var.name}-lambda"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}
#FIXME: replace with fine grained policy document
# KMS signing
# ParameterStore lookup
# Cloudwatch logging
# SecretsManager API key lookup
# CloudFront cache flush
# DynamoDB
# S3
resource "aws_iam_role_policy_attachment" "lambda_all" {
  count      = length(local.lambda_policies)
  policy_arn = local.lambda_policies[count.index]
  role       = aws_iam_role.this.name
}
