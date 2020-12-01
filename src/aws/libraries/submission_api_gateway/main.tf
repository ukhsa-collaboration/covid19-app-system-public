locals {
  identifier_prefix = "${terraform.workspace}-${var.name}"
}

data "aws_region" "current" {}
data "aws_caller_identity" "current" {}

resource "aws_apigatewayv2_api" "this" {
  name          = "${local.identifier_prefix}-http-api"
  protocol_type = "HTTP"

  tags = var.tags
}

resource "aws_apigatewayv2_integration" "this" {
  api_id             = aws_apigatewayv2_api.this.id
  integration_type   = "AWS_PROXY"
  integration_method = "POST"
  integration_uri    = var.lambda_function_arn
}

resource "aws_lambda_permission" "this" {
  statement_id  = "AllowAPIGatewayInvoke"
  function_name = "${var.lambda_function_name}${var.lambda_function_version != 0 ? format("%s%s", ":", var.lambda_function_version) : ""}"
  action        = "lambda:InvokeFunction"
  principal     = "apigateway.amazonaws.com"
  source_arn    = "arn:aws:execute-api:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:${aws_apigatewayv2_api.this.id}/*/*/{proxy+}"
}


resource "aws_apigatewayv2_route" "this" {
  api_id    = aws_apigatewayv2_api.this.id
  route_key = "POST /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.this.id}"
}

resource "aws_apigatewayv2_stage" "this" {
  api_id      = aws_apigatewayv2_api.this.id
  name        = "$default"
  auto_deploy = true

  tags = var.tags

  default_route_settings {
    detailed_metrics_enabled = true
    logging_level            = "OFF"
    throttling_burst_limit   = var.burst_limit
    throttling_rate_limit    = var.rate_limit
  }
}
