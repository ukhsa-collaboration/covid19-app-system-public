
resource "aws_dynamodb_table" "test_orders" {
  name         = "${local.identifier_prefix}-ordertokens"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "ctaToken"

  attribute {
    name = "ctaToken"
    type = "S"
  }
  ttl {
    attribute_name = "expireAt"
    enabled        = true
  }
}

resource "aws_dynamodb_table" "test_results" {
  name         = "${local.identifier_prefix}-testresults"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "testResultPollingToken"

  attribute {
    name = "testResultPollingToken"
    type = "S"
  }
  ttl {
    attribute_name = "expireAt"
    enabled        = true
  }
}

resource "aws_dynamodb_table" "submission_tokens" {
  name         = "${local.identifier_prefix}-submissiontokens"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "diagnosisKeySubmissionToken"

  attribute {
    name = "diagnosisKeySubmissionToken"
    type = "S"
  }
  ttl {
    attribute_name = "expireAt"
    enabled        = true
  }
}
