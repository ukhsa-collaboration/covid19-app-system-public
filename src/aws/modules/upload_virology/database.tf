
resource "aws_dynamodb_table" "test_orders" {
  name         = "${local.identifier_prefix}-ordertokens"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "ctaToken"
  tags         = var.tags
  point_in_time_recovery {
    enabled = true
  }

  attribute {
    name = "ctaToken"
    type = "S"
  }
  attribute {
    name = "testResultPollingToken"
    type = "S"
  }

  global_secondary_index {
    hash_key           = "testResultPollingToken"
    name               = "${local.identifier_prefix}-ordertokens-index"
    projection_type    = "INCLUDE"
    non_key_attributes = ["diagnosisKeySubmissionToken", "ctaToken"]
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
  tags         = var.tags
  point_in_time_recovery {
    enabled = true
  }

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
  tags         = var.tags
  point_in_time_recovery {
    enabled = true
  }

  attribute {
    name = "diagnosisKeySubmissionToken"
    type = "S"
  }
  ttl {
    attribute_name = "expireAt"
    enabled        = true
  }
}
