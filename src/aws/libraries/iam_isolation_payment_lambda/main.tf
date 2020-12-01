data "aws_iam_policy_document" "this" {
  statement {
    actions = [
      "lambda:InvokeFunction"
    ]
    resources = [
      var.consume_function_arn,
      var.verify_function_arn,
    ]
  }
}

resource "aws_iam_policy" "this" {
  name_prefix = "${var.name}-policy"
  path        = "/"
  policy      = data.aws_iam_policy_document.this.json
}

resource "aws_iam_role_policy_attachment" "this" {
  role       = aws_iam_role.this.name
  policy_arn = aws_iam_policy.this.arn
}

resource "aws_iam_role" "this" {
  name = var.name

  tags = var.tags

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "AWS": ${jsonencode(var.isolation_payment_gateway_role_trust_policy_principal)}
      },
      "Effect": "Allow"
    }
  ]
}
EOF
}
