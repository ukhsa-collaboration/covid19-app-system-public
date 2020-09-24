locals {
  lambda_policies = [
    "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole",
    "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess",
    "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess",
    "arn:aws:iam::aws:policy/AmazonSSMReadOnlyAccess",
    "arn:aws:iam::aws:policy/SecretsManagerReadWrite"
  ]

  identifier_prefix = "${terraform.workspace}-virology"
}

module "upload_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = "${local.identifier_prefix}-upload"
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.virology.VirologyUploadHandler"
  lambda_execution_role_arn = aws_iam_role.upload_lambda_execution_role.arn
  lambda_timeout            = 20
  lambda_memory             = 1024
  lambda_environment_variables = {
    test_orders_table       = aws_dynamodb_table.test_orders.id
    test_results_table      = aws_dynamodb_table.test_results.id
    submission_tokens_table = aws_dynamodb_table.submission_tokens.id
    test_orders_index       = "${local.identifier_prefix}-ordertokens-index"
    custom_oai              = var.custom_oai
  }
  app_alarms_topic = var.alarm_topic_arn
}

module "upload_gateway" {
  source               = "../../libraries/submission_api_gateway"
  name                 = "${local.identifier_prefix}-upload"
  lambda_function_arn  = module.upload_lambda.lambda_function_arn
  lambda_function_name = module.upload_lambda.lambda_function_name
  burst_limit          = var.burst_limit
  rate_limit           = var.rate_limit
}

resource "aws_iam_role" "upload_lambda_execution_role" {
  name = "${local.identifier_prefix}-upload-lambda"

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

resource "aws_iam_role_policy_attachment" "upload_lambda_execution_role" {
  count      = length(local.lambda_policies)
  policy_arn = local.lambda_policies[count.index]
  role       = aws_iam_role.upload_lambda_execution_role.name
}
