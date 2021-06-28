resource "random_uuid" "submission-custom-oai" {}

module "analytics_submission_fast_ingest" {
  source                   = "./modules/submission_analytics"
  name                     = "analytics"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_environment_variables = {
    firehose_stream_name    = aws_kinesis_firehose_delivery_stream.analytics_stream.name
    firehose_ingest_enabled = "true"
    custom_oai              = random_uuid.submission-custom-oai.result
  }
  burst_limit                    = var.burst_limit
  rate_limit                     = var.rate_limit
  alarm_topic_arn                = var.alarm_topic_arn
  reserved_concurrent_executions = var.analytics_queue_processor_reserved_concurrent_executions
  tags                           = var.tags
}

module "analytics_events_submission" {
  source                   = "./modules/submission"
  name                     = "analytics-events"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_handler_class     = "uk.nhs.nhsx.analyticsevents.AnalyticsEventsHandler"
  lambda_environment_variables = {
    SSM_KEY_ID_PARAMETER_NAME = "/app/kms/ContentSigningKeyArn"
    ACCEPT_REQUESTS_ENABLED   = true
    custom_oai                = random_uuid.submission-custom-oai.result
  }
  burst_limit              = var.burst_limit
  rate_limit               = var.rate_limit
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  log_retention_in_days    = var.log_retention_in_days
  alarm_topic_arn          = var.alarm_topic_arn
  policy_document          = module.analytics_events_submission_access.policy_document
  tags                     = var.tags
}

module "analytics_submission_store_parquet" {
  source                   = "./libraries/submission_s3"
  name                     = "analytics"
  service                  = "submission-parquet"
  logs_bucket_id           = null
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  policy_document          = module.analytics_submission_store_parquet_access.policy_document
  tags                     = var.tags
}

module "analytics_submission_store_parquet_consolidated" {
  source                   = "./libraries/submission_s3"
  name                     = "analytics-consolidated"
  service                  = "submission-parquet"
  logs_bucket_id           = null
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  policy_document          = module.analytics_submission_store_parquet_access_consolidated.policy_document
  tags                     = var.tags
}

############################

resource "aws_glue_catalog_database" "mobile_analytics" {
  name = "${terraform.workspace}-analytics"
}

# [PARQUET CONSOLIDATION] FIXME: columns need to be kept in sync with glue table below
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
      name = "localAuthority"
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
    columns {
      name = "receivedVoidTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedPositiveTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedNegativeTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedVoidTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedPositiveTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedNegativeTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "hasSelfDiagnosedBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasTestedPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForSelfDiagnosedBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForTestedPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForHadRiskyContactBackgroundTick"
      type = "int"
    }
    columns {
      name = "receivedRiskyContactNotification"
      type = "int"
    }
    columns {
      name = "startedIsolation"
      type = "int"
    }
    columns {
      name = "receivedPositiveTestResultWhenIsolatingDueToRiskyContact"
      type = "int"
    }
    columns {
      name = "receivedActiveIpcToken"
      type = "int"
    }
    columns {
      name = "haveActiveIpcTokenBackgroundTick"
      type = "int"
    }
    columns {
      name = "selectedIsolationPaymentsButton"
      type = "int"
    }
    columns {
      name = "launchedIsolationPaymentsApplication"
      type = "int"
    }
    columns {
      name = "receivedPositiveLFDTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedNegativeLFDTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedVoidLFDTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedPositiveLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedNegativeLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedVoidLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "hasTestedLFDPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForTestedLFDPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "totalExposureWindowsNotConsideredRisky"
      type = "int"
    }
    columns {
      name = "totalExposureWindowsConsideredRisky"
      type = "int"
    }
    columns {
      name = "acknowledgedStartOfIsolationDueToRiskyContact"
      type = "int"
    }
    columns {
      name = "hasRiskyContactNotificationsEnabledBackgroundTick"
      type = "int"
    }
    columns {
      name = "totalRiskyContactReminderNotifications"
      type = "int"
    }
    columns {
      name = "receivedUnconfirmedPositiveTestResult"
      type = "int"
    }
    columns {
      name = "isIsolatingForUnconfirmedTestBackgroundTick"
      type = "int"
    }
    columns {
      name = "launchedTestOrdering"
      type = "int"
    }
    columns {
      name = "didHaveSymptomsBeforeReceivedTestResult"
      type = "int"
    }
    columns {
      name = "didRememberOnsetSymptomsDateBeforeReceivedTestResult"
      type = "int"
    }
    columns {
      name = "didAskForSymptomsOnPositiveTestEntry"
      type = "int"
    }
    columns {
      name = "declaredNegativeResultFromDCT"
      type = "int"
    }
    columns {
      name = "receivedPositiveSelfRapidTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedNegativeSelfRapidTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedVoidSelfRapidTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedPositiveSelfRapidTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedNegativeSelfRapidTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedVoidSelfRapidTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "isIsolatingForTestedSelfRapidPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasTestedSelfRapidPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "receivedRiskyVenueM1Warning"
      type = "int"
    }
    columns {
      name = "receivedRiskyVenueM2Warning"
      type = "int"
    }
    columns {
      name = "hasReceivedRiskyVenueM2WarningBackgroundTick"
      type = "int"
    }
    columns {
      name = "totalAlarmManagerBackgroundTasks"
      type = "int"
    }
    columns {
      name = "missingPacketsLast7Days"
      type = "int"
    }
    columns {
      name = "consentedToShareVenueHistory"
      type = "int"
    }
    columns {
      name = "askedToShareVenueHistory"
      type = "int"
    }
    columns {
      name = "askedToShareExposureKeysInTheInitialFlow"
      type = "int"
    }
    columns {
      name = "consentedToShareExposureKeysInTheInitialFlow"
      type = "int"
    }
    columns {
      name = "totalShareExposureKeysReminderNotifications"
      type = "int"
    }
    columns {
      name = "consentedToShareExposureKeysInReminderScreen"
      type = "int"
    }
    columns {
      name = "successfullySharedExposureKeys"
      type = "int"
    }
    columns {
      name = "didSendLocalInfoNotification"
      type = "int"
    }
    columns {
      name = "didAccessLocalInfoScreenViaNotification"
      type = "int"
    }
    columns {
      name = "didAccessLocalInfoScreenViaBanner"
      type = "int"
    }
    columns {
      name = "isDisplayingLocalInfoBackgroundTick"
      type = "int"
    }
    columns {
      name = "positiveLabResultAfterPositiveLFD"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveLFDWithinTimeLimit"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveLFDOutsideTimeLimit"
      type = "int"
    }
    columns {
      name = "positiveLabResultAfterPositiveSelfRapidTest"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit"
      type = "int"
    }
  }
}

# [PARQUET CONSOLIDATION] FIXME: columns need to be kept in sync with glue table above
resource "aws_glue_catalog_table" "mobile_analytics_consolidated" {
  name          = "${terraform.workspace}_analytics_consolidated"
  database_name = aws_glue_catalog_database.mobile_analytics.name

  table_type = "EXTERNAL_TABLE"

  parameters = {
    EXTERNAL              = "TRUE"
    "parquet.compression" = "SNAPPY"
  }

  storage_descriptor {
    location      = "s3://${module.analytics_submission_store_parquet_consolidated.bucket_id}/"
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
      name = "localAuthority"
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
    columns {
      name = "receivedVoidTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedPositiveTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedNegativeTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedVoidTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedPositiveTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedNegativeTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "hasSelfDiagnosedBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasTestedPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForSelfDiagnosedBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForTestedPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForHadRiskyContactBackgroundTick"
      type = "int"
    }
    columns {
      name = "receivedRiskyContactNotification"
      type = "int"
    }
    columns {
      name = "startedIsolation"
      type = "int"
    }
    columns {
      name = "receivedPositiveTestResultWhenIsolatingDueToRiskyContact"
      type = "int"
    }
    columns {
      name = "receivedActiveIpcToken"
      type = "int"
    }
    columns {
      name = "haveActiveIpcTokenBackgroundTick"
      type = "int"
    }
    columns {
      name = "selectedIsolationPaymentsButton"
      type = "int"
    }
    columns {
      name = "launchedIsolationPaymentsApplication"
      type = "int"
    }
    columns {
      name = "receivedPositiveLFDTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedNegativeLFDTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedVoidLFDTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedPositiveLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedNegativeLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedVoidLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "hasTestedLFDPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForTestedLFDPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "totalExposureWindowsNotConsideredRisky"
      type = "int"
    }
    columns {
      name = "totalExposureWindowsConsideredRisky"
      type = "int"
    }
    columns {
      name = "acknowledgedStartOfIsolationDueToRiskyContact"
      type = "int"
    }
    columns {
      name = "hasRiskyContactNotificationsEnabledBackgroundTick"
      type = "int"
    }
    columns {
      name = "totalRiskyContactReminderNotifications"
      type = "int"
    }
    columns {
      name = "receivedUnconfirmedPositiveTestResult"
      type = "int"
    }
    columns {
      name = "isIsolatingForUnconfirmedTestBackgroundTick"
      type = "int"
    }
    columns {
      name = "launchedTestOrdering"
      type = "int"
    }
    columns {
      name = "didHaveSymptomsBeforeReceivedTestResult"
      type = "int"
    }
    columns {
      name = "didRememberOnsetSymptomsDateBeforeReceivedTestResult"
      type = "int"
    }
    columns {
      name = "didAskForSymptomsOnPositiveTestEntry"
      type = "int"
    }
    columns {
      name = "declaredNegativeResultFromDCT"
      type = "int"
    }
    columns {
      name = "receivedPositiveSelfRapidTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedNegativeSelfRapidTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedVoidSelfRapidTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedPositiveSelfRapidTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedNegativeSelfRapidTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedVoidSelfRapidTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "isIsolatingForTestedSelfRapidPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasTestedSelfRapidPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "receivedRiskyVenueM1Warning"
      type = "int"
    }
    columns {
      name = "receivedRiskyVenueM2Warning"
      type = "int"
    }
    columns {
      name = "hasReceivedRiskyVenueM2WarningBackgroundTick"
      type = "int"
    }
    columns {
      name = "totalAlarmManagerBackgroundTasks"
      type = "int"
    }
    columns {
      name = "missingPacketsLast7Days"
      type = "int"
    }
    columns {
      name = "consentedToShareVenueHistory"
      type = "int"
    }
    columns {
      name = "askedToShareVenueHistory"
      type = "int"
    }
    columns {
      name = "askedToShareExposureKeysInTheInitialFlow"
      type = "int"
    }
    columns {
      name = "consentedToShareExposureKeysInTheInitialFlow"
      type = "int"
    }
    columns {
      name = "totalShareExposureKeysReminderNotifications"
      type = "int"
    }
    columns {
      name = "consentedToShareExposureKeysInReminderScreen"
      type = "int"
    }
    columns {
      name = "successfullySharedExposureKeys"
      type = "int"
    }
    columns {
      name = "didSendLocalInfoNotification"
      type = "int"
    }
    columns {
      name = "didAccessLocalInfoScreenViaNotification"
      type = "int"
    }
    columns {
      name = "didAccessLocalInfoScreenViaBanner"
      type = "int"
    }
    columns {
      name = "isDisplayingLocalInfoBackgroundTick"
      type = "int"
    }
    columns {
      name = "positiveLabResultAfterPositiveLFD"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveLFDWithinTimeLimit"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveLFDOutsideTimeLimit"
      type = "int"
    }
    columns {
      name = "positiveLabResultAfterPositiveSelfRapidTest"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit"
      type = "int"
    }
  }
}

resource "aws_iam_role" "firehose_role" {
  name = "${terraform.workspace}-firehose-analytics"

  tags = var.tags

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

locals {
  env_analytics_submission                = lookup(var.analytics_submission, terraform.workspace, var.analytics_submission["default"])
  analytics_submission_ingestion_interval = tonumber(local.env_analytics_submission["ingestion_interval"])
  virology_submission_config              = lookup(var.virology_submission, terraform.workspace, var.virology_submission["default"])
}

resource "aws_kinesis_firehose_delivery_stream" "analytics_stream" {
  name        = "${terraform.workspace}-analytics"
  destination = "extended_s3"

  tags = var.tags

  extended_s3_configuration {
    role_arn = aws_iam_role.firehose_role.arn

    # [PARQUET CONSOLIDATION] FIXME below: the following configuration must be active during the initial migration (parquet consolidation)
    bucket_arn = module.analytics_submission_store_parquet.bucket_arn

    # [PARQUET CONSOLIDATION] FIXME below: the following configuration must be activated before the delta migration (parquet consolidation) is started
    # bucket_arn          = module.analytics_submission_store_parquet_consolidated.bucket_arn
    # prefix              = "submitteddatehour=!{timestamp:yyyy}-!{timestamp:MM}-!{timestamp:dd}-!{timestamp:HH}/"
    # error_output_prefix = "firehose-failures/!{firehose:error-output-type}/"

    buffer_interval = local.analytics_submission_ingestion_interval
    buffer_size     = 128

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

        # [PARQUET CONSOLIDATION] FIXME below: the following configuration must be active during the initial migration (parquet consolidation)
        table_name = aws_glue_catalog_table.mobile_analytics.name

        # [PARQUET CONSOLIDATION] FIXME below: the following configuration must be activated before the delta migration (parquet consolidation) is started
        # table_name = aws_glue_catalog_table.mobile_analytics_consolidated.name
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
  lambda_handler_class     = "uk.nhs.nhsx.diagnosiskeyssubmission.DiagnosisKeySubmissionHandler"
  lambda_environment_variables = {
    SSM_KEY_ID_PARAMETER_NAME = "/app/kms/ContentSigningKeyArn"
    submission_tokens_table   = module.virology_upload.submission_tokens_table
    custom_oai                = random_uuid.submission-custom-oai.result
  }
  burst_limit              = var.burst_limit
  rate_limit               = var.rate_limit
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  log_retention_in_days    = var.log_retention_in_days
  alarm_topic_arn          = var.alarm_topic_arn
  replication_enabled      = var.submission_replication_enabled
  lifecycle_rule_enabled   = true
  policy_document          = module.diagnosis_keys_submission_access.policy_document
  tags                     = var.tags
}

module "empty_submission" {
  source                   = "./modules/submission"
  name                     = "empty-submission"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_handler_class     = "uk.nhs.nhsx.emptysubmission.EmptySubmissionHandler"
  lambda_environment_variables = {
    SSM_KEY_ID_PARAMETER_NAME = "/app/kms/ContentSigningKeyArn"
    custom_oai                = random_uuid.submission-custom-oai.result
  }
  burst_limit              = var.burst_limit
  rate_limit               = var.rate_limit
  logs_bucket_id           = var.logs_bucket_id
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  log_retention_in_days    = var.log_retention_in_days
  alarm_topic_arn          = var.alarm_topic_arn
  replication_enabled      = var.submission_replication_enabled
  lifecycle_rule_enabled   = true
  policy_document          = module.empty_submission_access.policy_document
  tags                     = var.tags
}

module "empty_submission_v2" {
  source                   = "./modules/submission_empty"
  name                     = "empty-submission-v2"
  tags                     = var.tags
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  policy_document          = module.empty_submission_v2_distribution_access.policy_document
}

module "virology_submission" {
  source                              = "./modules/submission_virology"
  lambda_repository_bucket            = module.artifact_repository.bucket_name
  lambda_object_key                   = module.artifact_repository.lambda_object_key
  burst_limit                         = var.burst_limit
  rate_limit                          = var.rate_limit
  test_order_website                  = local.virology_submission_config["virology_test_order_website"]
  test_register_website               = local.virology_submission_config["virology_test_register_website"]
  test_orders_table_id                = module.virology_upload.test_orders_table
  test_results_table_id               = module.virology_upload.results_table
  virology_submission_tokens_table_id = module.virology_upload.submission_tokens_table
  test_orders_index                   = module.virology_upload.test_orders_index_name
  custom_oai                          = random_uuid.submission-custom-oai.result
  log_retention_in_days               = var.log_retention_in_days
  alarm_topic_arn                     = var.alarm_topic_arn
  tags                                = var.tags
}

module "isolation_payment_submission" {
  source                          = "./modules/submission_isolation_payment"
  lambda_repository_bucket        = module.artifact_repository.bucket_name
  lambda_object_key               = module.artifact_repository.lambda_object_key
  burst_limit                     = var.burst_limit
  rate_limit                      = var.rate_limit
  isolation_token_expiry_in_weeks = var.isolation_token_expiry_in_weeks
  configuration                   = lookup(var.isolation_payment, terraform.workspace, var.isolation_payment["default"])
  custom_oai                      = random_uuid.submission-custom-oai.result
  log_retention_in_days           = var.log_retention_in_days
  alarm_topic_arn                 = var.alarm_topic_arn
  tags                            = var.tags
}

module "crash_reports_submission" {
  source                   = "./modules/submission_crash_reports"
  name                     = "crash-reports"
  lambda_repository_bucket = module.artifact_repository.bucket_name
  lambda_object_key        = module.artifact_repository.lambda_object_key
  lambda_handler_class     = "uk.nhs.nhsx.crashreports.CrashReportsHandler"
  lambda_environment_variables = {
    custom_oai         = random_uuid.submission-custom-oai.result,
    SUBMISSION_ENABLED = true
  }
  burst_limit           = var.burst_limit
  rate_limit            = var.rate_limit
  log_retention_in_days = 7
  alarm_topic_arn       = var.alarm_topic_arn
  tags                  = var.tags
}

module "submission_apis" {
  source = "./libraries/cloudfront_submission_facade"

  name        = "submission"
  domain      = var.base_domain
  web_acl_arn = var.waf_arn

  analytics_submission_endpoint                   = module.analytics_submission_fast_ingest.endpoint
  analytics_submission_path                       = "/submission/mobile-analytics"
  analytics_submission_health_path                = "/submission/mobile-analytics/health"
  analytics_events_submission_endpoint            = module.analytics_events_submission.endpoint
  analytics_events_submission_path                = "/submission/mobile-analytics-events"
  analytics_events_submission_health_path         = "/submission/mobile-analytics-events/health"
  diagnosis_keys_submission_endpoint              = module.diagnosis_keys_submission.endpoint
  diagnosis_keys_submission_path                  = "/submission/diagnosis-keys"
  diagnosis_keys_submission_health_path           = "/submission/diagnosis-keys/health"
  empty_submission_endpoint                       = module.empty_submission.endpoint
  empty_submission_path                           = "/submission/empty-submission"
  empty_submission_v2_bucket_regional_domain_name = module.empty_submission_v2.store_regional_domain_name
  empty_submission_v2_origin_access_identity_path = module.empty_submission_v2.origin_access_identity_path
  empty_submission_v2_path                        = "/submission/empty-submission-v2"
  crash_reports_submission_endpoint               = module.crash_reports_submission.endpoint
  crash_reports_submission_path                   = "/submission/crash-reports"
  exposure_notification_circuit_breaker_endpoint  = module.exposure_notification_circuit_breaker.endpoint
  exposure_notification_circuit_breaker_path      = "/circuit-breaker/exposure-notification/*"
  isolation_payment_endpoint                      = module.isolation_payment_submission.endpoint
  isolation_payment_path                          = "/isolation-payment/ipc-token/*"
  isolation_payment_health_path                   = "/isolation-payment/health"
  risky_venues_circuit_breaker_endpoint           = module.risky_venues_circuit_breaker.endpoint
  risky_venues_circuit_breaker_path               = "/circuit-breaker/venue/*"
  virology_kit_endpoint                           = module.virology_submission.api_endpoint
  virology_kit_path                               = "/virology-test/*"
  custom_oai                                      = random_uuid.submission-custom-oai.result
  enable_shield_protection                        = var.enable_shield_protection
  tags                                            = var.tags
}
############################

output "analytics_submission_parquet_store" {
  value = module.analytics_submission_store_parquet.bucket_name
}
output "analytics_submission_ingestion_interval" {
  value = local.analytics_submission_ingestion_interval
}
output "analytics_events_submission_store" {
  value = module.analytics_events_submission.store
}
output "analytics_events_submission_function" {
  value = module.analytics_events_submission.function
}
output "diagnosis_keys_submission_store" {
  value = module.diagnosis_keys_submission.store
}
output "analytics_submission_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/mobile-analytics"
}
output "analytics_events_submission_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/mobile-analytics-events"
}
output "diagnosis_keys_submission_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/diagnosis-keys"
}
output "crash_reports_submission_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/crash-reports"
}
output "empty_submission_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/empty-submission"
}
output "empty_submission_v2_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/empty-submission-v2"
}
output "virology_kit_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/virology-test"
}
output "isolation_payment_create_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/isolation-payment/ipc-token/create"
}
output "isolation_payment_update_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/isolation-payment/ipc-token/update"
}
output "isolation_payment_create_gateway_endpoint" {
  value = "${module.isolation_payment_submission.endpoint}/isolation-payment/ipc-token/create"
}
output "isolation_payment_update_gateway_endpoint" {
  value = "${module.isolation_payment_submission.endpoint}/isolation-payment/ipc-token/update"
}
output "risky_venues_history_submission_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/risky-venue-history"
}

output "virology_submission_lambda_function_name" {
  value = module.virology_submission.lambda_function_name
}
output "isolation_payment_order_lambda_function_name" {
  value = module.isolation_payment_submission.order_lambda_function_name
}
output "isolation_payment_verify_lambda_function_name" {
  value = module.isolation_payment_submission.verify_lambda_function_name
}
output "isolation_payment_consume_lambda_function_name" {
  value = module.isolation_payment_submission.consume_lambda_function_name
}
output "isolation_payment_tokens_table" {
  value = module.isolation_payment_submission.ipc_tokens_table
}

# Health endpoints
output "analytics_submission_health_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/mobile-analytics/health"
}
output "analytics_events_submission_health_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/mobile-analytics-events/health"
}
output "diagnosis_keys_submission_health_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/submission/diagnosis-keys/health"
}
output "isolation_payment_health_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/isolation-payment/health"
}
output "virology_kit_health_endpoint" {
  value = "https://${module.submission_apis.submission_domain_name}/virology-test/health"
}
output "virology_kit_submission_gateway_endpoint" {
  value = module.virology_submission.api_endpoint
}
