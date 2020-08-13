locals {
  identifier_prefix = "${terraform.workspace}-${var.name}-sub"
}

data "aws_caller_identity" "caller" {}
data "aws_region" "current" {}

module "submission_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.activationsubmission.Handler"
  lambda_execution_role_arn = aws_iam_role.this.arn
  lambda_timeout            = 10
  lambda_memory             = 2048
  lambda_environment_variables = {
    SSM_KEY_ID_PARAMETER_NAME = "/app/kms/ContentSigningKeyArn",
  }
}

module "submission_gateway" {
  source               = "../../libraries/submission_api_gateway"
  name                 = var.name
  lambda_function_arn  = module.submission_lambda.lambda_function_arn
  lambda_function_name = module.submission_lambda.lambda_function_name
  burst_limit          = var.burst_limit
  rate_limit           = var.rate_limit
}

resource "aws_iam_role" "this" {
  name = "${local.identifier_prefix}-lambda"

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

data "aws_iam_policy_document" "this" {

  statement {
    actions = ["kms:Sign"]
    // key * isn't ideal, but we don't know the arn at this point.
    resources = ["arn:aws:kms:${data.aws_region.current.name}:${data.aws_caller_identity.caller.account_id}:key/*"]
  }

  statement {
    actions   = ["ssm:GetParameter"]
    resources = ["arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.caller.account_id}:parameter/app/*"]
  }

  statement {
    actions   = ["secretsmanager:GetSecretValue"]
    resources = ["arn:aws:secretsmanager:${data.aws_region.current.name}:${data.aws_caller_identity.caller.account_id}:secret:/mobile/*"]
  }

  statement {
    actions = [
      "dynamodb:GetItem",
      "dynamodb:UpdateItem"
    ]
    resources = [
      "arn:aws:dynamodb:${data.aws_region.current.name}:${data.aws_caller_identity.caller.account_id}:table/*-ActivationCodes",
      "arn:aws:dynamodb:${data.aws_region.current.name}:${data.aws_caller_identity.caller.account_id}:table/*-ActivationCode-Reporting",
    ]
  }

  statement {
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]

    resources = [
      "arn:aws:logs:*:*:*",
    ]
  }
}

resource "aws_dynamodb_table" "test_orders" {
  name         = "${terraform.workspace}-ActivationCodes"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "Code"

  attribute {
    name = "Code"
    type = "S"
  }
}

resource "aws_dynamodb_table" "activation_code_reporting" {
  name         = "${terraform.workspace}-ActivationCode-Reporting"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "Date"
  range_key    = "Batch"

  attribute {
    name = "Date"
    type = "S"
  }
  attribute {
    name = "Batch"
    type = "S"
  }
}

resource "aws_iam_policy" "this" {
  name_prefix = "${local.identifier_prefix}-lambda"
  path        = "/"
  policy      = data.aws_iam_policy_document.this.json
}

resource "aws_iam_role_policy_attachment" "this" {
  role       = aws_iam_role.this.name
  policy_arn = aws_iam_policy.this.arn
}

resource "aws_iam_role_policy_attachment" "lambda_xray" {
  role       = aws_iam_role.this.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

resource "aws_iam_role_policy_attachment" "submission_lambda_policy_attachment" {
  role       = aws_iam_role.this.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}
