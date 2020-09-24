resource "random_uuid" "submission-custom-oai" {}

module "analytics_submission" {
  source                   = "./modules/submission"
  name                     = "analytics"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_handler_class     = "uk.nhs.nhsx.analyticssubmission.Handler"
  lambda_environment_variables = {
    firehose_stream_name    = "${terraform.workspace}-analytics"
    s3_ingest_enabled       = "false"
    firehose_ingest_enabled = "true"
    custom_oai              = random_uuid.submission-custom-oai.result
  }
  burst_limit              = var.burst_limit
  rate_limit               = var.rate_limit
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  alarm_topic_arn          = var.alarm_topic_arn
}

module "analytics_submission_store_parquet" {
  source                   = "./libraries/submission_s3"
  name                     = "analytics"
  service                  = "submission-parquet"
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
}

############################

resource "aws_glue_catalog_database" "mobile_analytics" {
  name = "${terraform.workspace}-analytics"
}

resource "aws_glue_catalog_table" "mobile_analytics" {
  name          = "${terraform.workspace}_analytics"
  database_name = aws_glue_catalog_database.mobile_analytics.name

  table_type = "EXTERNAL_TABLE"

  parameters = {
    EXTERNAL              = "TRUE"
    "parquet.compression" = "SNAPPY"
  }

  storage_descriptor {
    location      = "s3://${module.analytics_submission_store_parquet.bucket_id}/"
    input_format  = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat"

    ser_de_info {
      name                  = "my-stream"
      serialization_library = "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe"

      parameters = {
        "serialization.format" = 1
      }
    }

    columns {
      name = "startDate"
      type = "string"
    }

    columns {
      name = "endDate"
      type = "string"
    }
    columns {
      name = "postalDistrict"
      type = "string"
    }
    columns {
      name = "deviceModel"
      type = "string"
    }
    columns {
      name = "latestApplicationVersion"
      type = "string"
    }
    columns {
      name = "operatingSystemVersion"
      type = "string"
    }
    columns {
      name = "cumulativeDownloadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeUploadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeCellularDownloadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeCellularUploadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeWifiDownloadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeWifiUploadBytes"
      type = "int"
    }
    columns {
      name = "checkedIn"
      type = "int"
    }
    columns {
      name = "canceledCheckIn"
      type = "int"
    }
    columns {
      name = "receivedVoidTestResult"
      type = "int"
    }

    columns {
      name = "isIsolatingBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasHadRiskyContactBackgroundTick"
      type = "int"
    }

    columns {
      name = "receivedPositiveTestResult"
      type = "int"
    }
    columns {
      name = "receivedNegativeTestResult"
      type = "int"
    }

    columns {
      name = "hasSelfDiagnosedPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "completedQuestionnaireAndStartedIsolation"
      type = "int"
    }

    columns {
      name = "encounterDetectionPausedBackgroundTick"
      type = "int"
    }
    columns {
      name = "completedQuestionnaireButDidNotStartIsolation"
      type = "int"
    }

    columns {
      name = "totalBackgroundTasks"
      type = "int"
    }
    columns {
      name = "runningNormallyBackgroundTick"
      type = "int"
    }

    columns {
      name = "completedOnboarding"
      type = "int"
    }
    columns {
      name = "includesMultipleApplicationVersions"
      type = "boolean"
    }
  }
}

resource "aws_iam_role" "firehose_role" {
  name = "${terraform.workspace}-firehose-analytics"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "firehose.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "firehose_s3" {
  role       = aws_iam_role.firehose_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

resource "aws_iam_role_policy_attachment" "firehose_glue" {
  role       = aws_iam_role.firehose_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSGlueServiceRole"
}

resource "aws_kinesis_firehose_delivery_stream" "analytics_stream" {
  name        = "${terraform.workspace}-analytics"
  destination = "extended_s3"

  extended_s3_configuration {
    role_arn        = aws_iam_role.firehose_role.arn
    bucket_arn      = module.analytics_submission_store_parquet.bucket_arn
    buffer_interval = 60
    buffer_size     = 64

    data_format_conversion_configuration {
      input_format_configuration {
        deserializer {
          open_x_json_ser_de {}
        }
      }

      output_format_configuration {
        serializer {
          parquet_ser_de {}
        }
      }

      schema_configuration {
        database_name = aws_glue_catalog_table.mobile_analytics.database_name
        role_arn      = aws_iam_role.firehose_role.arn
        table_name    = aws_glue_catalog_table.mobile_analytics.name
      }
    }
  }
}

############################

module "diagnosis_keys_submission" {
  source                   = "./modules/submission"
  name                     = "diagnosis-keys"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_handler_class     = "uk.nhs.nhsx.diagnosiskeyssubmission.Handler"
  lambda_environment_variables = {
    SSM_KEY_ID_PARAMETER_NAME = "/app/kms/ContentSigningKeyArn"
    submission_tokens_table   = module.virology_upload.submission_tokens_table
    custom_oai                = random_uuid.submission-custom-oai.result
  }
  burst_limit              = var.burst_limit
  rate_limit               = var.rate_limit
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  alarm_topic_arn          = var.alarm_topic_arn
}

module "activation_keys_submission" {
  source                   = "./modules/submission_activation_keys"
  name                     = "activation-keys"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  burst_limit              = var.burst_limit
  rate_limit               = var.rate_limit
  custom_oai               = random_uuid.submission-custom-oai.result
  alarm_topic_arn          = var.alarm_topic_arn
}

module "virology_submission" {
  source                              = "./modules/submission_virology"
  lambda_repository_bucket            = module.artifact_repository.bucket_name
  lambda_object_key                   = module.artifact_repository.lambda_object_key
  burst_limit                         = var.burst_limit
  rate_limit                          = var.rate_limit
  test_order_website                  = var.virology_test_order_website
  test_register_website               = var.virology_test_register_website
  test_orders_table_id                = module.virology_upload.test_orders_table
  test_results_table_id               = module.virology_upload.results_table
  virology_submission_tokens_table_id = module.virology_upload.submission_tokens_table
  test_orders_index                   = module.virology_upload.test_orders_index_name
  custom_oai                          = random_uuid.submission-custom-oai.result
  alarm_topic_arn                     = var.alarm_topic_arn
}

module "submission_apis" {
  source = "./libraries/cloudfront_submission_facade"

  name        = "submission"
  domain      = var.base_domain
  web_acl_arn = data.aws_wafv2_web_acl.this.arn

  analytics_submission_endpoint                  = module.analytics_submission.endpoint
  analytics_submission_path                      = "/submission/mobile-analytics"
  analytics_submission_health_path               = "/submission/mobile-analytics/health"
  diagnosis_keys_submission_endpoint             = module.diagnosis_keys_submission.endpoint
  diagnosis_keys_submission_path                 = "/submission/diagnosis-keys"
  diagnosis_keys_submission_health_path          = "/submission/diagnosis-keys/health"
  exposure_notification_circuit_breaker_endpoint = module.exposure_notification_circuit_breaker.endpoint
  exposure_notification_circuit_breaker_path     = "/circuit-breaker/exposure-notification/*"
  risky_venues_circuit_breaker_endpoint          = module.risky_venues_circuit_breaker.endpoint
  risky_venues_circuit_breaker_path              = "/circuit-breaker/venue/*"
  virology_kit_endpoint                          = module.virology_submission.api_endpoint
  virology_kit_path                              = "/virology-test/*"
  activation_keys_submission_endpoint            = module.activation_keys_submission.endpoint
  activation_keys_submission_path                = "/activation/request"
  activation_keys_submission_health_path         = "/activation/request/health"
  custom_oai                                     = random_uuid.submission-custom-oai.result
  enable_shield_protection                       = var.enable_shield_protection
}

output "analytics_submission_store" {
  value = module.analytics_submission.store
}
output "diagnosis_keys_submission_store" {
  value = module.diagnosis_keys_submission.store
}
output "analytics_submission_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/mobile-analytics"
}
output "diagnosis_keys_submission_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/diagnosis-keys"
}
output "virology_kit_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/virology-test"
}
output "activation_keys_submission_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/activation/request"
}
output "virology_submission_lambda_function_name" {
  value = module.virology_submission.lambda_function_name
}

# Health endpoints
output "activation_keys_submission_health_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/activation/request/health"
}
output "analytics_submission_health_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/mobile-analytics/health"
}
output "diagnosis_keys_submission_health_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/diagnosis-keys/health"
}
output "virology_kit_health_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/virology-test/health"
}