data "aws_region" "current" {}
data "aws_caller_identity" "current" {}

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

resource "aws_iam_role_policy_attachment" "advanced_analytics_lambda_policy_attachment" {
  role       = aws_iam_role.this.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "lambda_xray" {
  role       = aws_iam_role.this.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXrayFullAccess"
}

resource "aws_iam_policy" "lambda_s3_policy" {
  name        = "${var.name}-s3"
  description = "Allow ${var.name} lambda to Get:Object from bucket ${var.bucket_name}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "s3:GetObject",
      "Effect": "Allow",
      "Resource": "arn:aws:s3:::${var.bucket_name}/*"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "lambda_s3_policy_attachment" {
  role       = aws_iam_role.this.name
  policy_arn = aws_iam_policy.lambda_s3_policy.arn
}

resource "aws_iam_policy" "lambda_secretsmanager_policy" {
  name        = "${var.name}-secretsmanager"
  description = "Allow ${var.name} lambda to read from secrets manager"

  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": "secretsmanager:GetSecretValue",
            "Resource": [
                "${var.certificate_secret_arn}",
                "${var.key_secret_name_arn}",
                "${var.encryption_password_secret_name_arn}",
                "${var.subscription_key_name_arn}"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "lambda_s3_getObject_policy" {
  role       = aws_iam_role.this.name
  policy_arn = aws_iam_policy.lambda_secretsmanager_policy.arn
}


